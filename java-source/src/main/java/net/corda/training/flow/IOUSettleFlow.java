package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;

import java.lang.IllegalArgumentException;
import java.util.*;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.workflows.GetBalances.getCashBalance;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IOUSettleFlow {

    /**
     * This is the flow which handles the settlement (partial or complete) of existing IOUs on the ledger.
     *これは、元帳上の既存のIOUの決済（部分的または完全）を処理するフローです。
     * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
     *取引相手の署名の収集は[CollectSignaturesFlow]によって処理されます。
     * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
     *公証（必要な場合）および元帳へのコミットメントは、[FinalityFlow]によって処理されます。
     * The flow returns the [SignedTransaction] that was committed to the ledger.
     *フローは、レジャーにコミットされた[SignedTransaction]を返します。
     */
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier stateLinearId;
        private final Amount<Currency> amount;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Amount<Currency> amount) {
            this.stateLinearId = stateLinearId;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            // 1. LinearStateQueryCriteriaを使用してボールトからIOU状態を取得する
            List<UUID> listOfLinearIds = Arrays.asList(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);

            // 2. Get a reference to the inputState data that we are going to settle.
            // 2.解決しようとしているinputStateデータへの参照を取得します。
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToSettle = (IOUState) ((StateAndRef) results.getStates().get(0)).getState().getData();

            // 3. Check the party running this flow is the borrower.
            // 3.このフローを実行しているパーティが借り手であることを確認します。
            if (!inputStateToSettle.borrower.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new IllegalArgumentException("The borrower must issue the flow");
            }

            // 4. We should now get some of the components required for to execute the transaction
            // 4.ここで、トランザクションの実行に必要なコンポーネントの一部を取得する必要があります
            // Here we get a reference to the default notary and instantiate a transaction builder.
            //ここでは、デフォルトの公証人への参照を取得し、トランザクションビルダーをインスタンス化します。
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder tb = new TransactionBuilder(notary);

            // 5. Check we have enough cash to settle the requested amount
            // 5.要求された金額を決済するのに十分な現金があることを確認します
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) amount.getToken());

            if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
            } else if (amount.getQuantity() > (inputStateToSettle.amount.getQuantity() - inputStateToSettle.paid.getQuantity())) {
                throw new IllegalArgumentException("Borrow tried to settle with more than was required for the obligation.");
            }

            // 6. Get some cash from the vault and add a spend to our transaction builder.
            // 6.ボールトから現金を受け取り、トランザクションビルダーに支出を追加します。
            CashUtils.generateSpend(getServiceHub(), tb, amount, getOurIdentityAndCert(), inputStateToSettle.lender, ImmutableSet.of()).getSecond();

            // 7. Create a command. you will need to provide the Command constructor with a reference to the Settle Command as well as a list of required signers.
            // 7.コマンドを作成します。 CommandコンストラクターにSettle Commandへの参照と必要な署名者のリストを提供する必要があります。
            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    inputStateToSettle.getParticipants()
                            .stream().map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())
            );

            // 8. Add the command and the input state to the transaction using the TransactionBuilder.
            // 8. TransactionBuilderを使用して、コマンドと入力状態をトランザクションに追加します。
            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // 9. Add an IOU output state if the IOU in question that has not been fully settled.
            // 9.問題のIOUが完全に解決されていない場合、IOU出力状態を追加します。
            if (amount.getQuantity() < inputStateToSettle.amount.getQuantity()) {
                tb.addOutputState(inputStateToSettle.pay(amount), IOUContract.IOU_CONTRACT_ID);
            }

            // 10. Verify and sign the transaction
            // 10.トランザクションを確認して署名する
            tb.verify(getServiceHub());
            SignedTransaction stx = getServiceHub().signInitialTransaction(tb, getOurIdentity().getOwningKey());

            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            // 11. CollectSignaturesFlowを使用して、他のCordaノードから必要な署名をすべて収集します
            List<FlowSession> sessions = new ArrayList<>();

            for (AbstractParty participant: inputStateToSettle.getParticipants()) {
                Party partyToInitiateFlow = (Party) participant;
                if (!partyToInitiateFlow.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                    sessions.add(initiateFlow(partyToInitiateFlow));
                }
            }
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(stx, sessions));

            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             *12.検証のためにトランザクションを公証人に送信するFinalityFlowの出力を返します。これにより
             *　　適切なノードのボールトに永続化されます。
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, sessions));

        }

    }

    /**
     * This is the flow which signs IOU settlements.
     *これは、IOU決済に署名するフローです。
     * The signing is handled by the [SignTransactionFlow].
     *署名は[SignTransactionFlow]によって処理されます。
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;
        private SecureHash txWeJustSignedId;

        public Responder(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().outputsOfType(IOUState.class).get(0);
                        require.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    //トランザクションが検証されたら、txWeJustSignedID変数を初期化します。
                    txWeJustSignedId = stx.getId();
                }
            }

            // Create a sign transaction flow
            //署名トランザクションフローを作成します
            SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flow to sign the transaction
            //署名トランザクションフローを実行して、トランザクションに署名します
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            // ReceiveFinalityFlowを実行してトランザクションを終了し、ボールトに永続化します。
            return subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSignedId));

        }
    }

    /**
     * Self issues the calling node an amount of cash in the desired currency.
     *自己は、呼び出し側ノードに希望する通貨で現金を発行します。
     * Only used for demo/sample/training purposes!
     *デモ/サンプル/トレーニングの目的でのみ使用されます！
     */

    @InitiatingFlow
    @StartableByRPC
    public static class SelfIssueCashFlow extends FlowLogic<Cash.State> {

        Amount<Currency> amount;

        SelfIssueCashFlow(Amount<Currency> amount) {
            this.amount = amount;
        }

        @Suspendable
        @Override
        public Cash.State call() throws FlowException {
            // Create the cash issue command.
            //キャッシュ発行コマンドを作成します。
            OpaqueBytes issueRef = OpaqueBytes.of(new byte[0]);
            // Note: ongoing work to support multiple notary identities is still in progress. */
            //注：複数のノータリーのアイデンティティをサポートするための作業はまだ進行中です。 * /
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // Create the cash issuance transaction.
            //現金発行トランザクションを作成します。
            AbstractCashFlow.Result cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary));
            return (Cash.State) cashIssueTransaction.getStx().getTx().getOutput(0);
        }

    }

}
