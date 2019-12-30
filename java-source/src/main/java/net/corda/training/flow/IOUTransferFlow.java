package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.utilities.ProgressTracker;
import net.corda.training.contract.IOUContract.Commands.Transfer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;

import javax.validation.constraints.NotNull;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;


/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 *これは、元帳上の既存のIOUの転送を処理するフローです。
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 *取引相手の署名の収集は[CollectSignaturesFlow]によって処理されます。
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 *公証（必要な場合）および元帳へのコミットメントは、[FinalityFlow]によって処理されます。
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 *フローは、レジャーにコミットされた[SignedTransaction]を返します。
 */
public class IOUTransferFlow{

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final UniqueIdentifier stateLinearId;
        private final Party newLender;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Party newLender) {
            this.stateLinearId = stateLinearId;
            this.newLender = newLender;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            // 1. LinearStateQueryCriteriaを使用して、ボールトからIOU状態を取得します
            List<UUID> listOfLinearIds = new ArrayList<>();
            listOfLinearIds.add(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);

            // 2. Get a reference to the inputState data that we are going to settle.
            // 2.解決しようとしているinputStateデータへの参照を取得します。
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToTransfer = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToTransfer = (IOUState) inputStateAndRefToTransfer.getState().getData();

            // 3. We should now get some of the components required for to execute the transaction
            // 3.トランザクションを実行するために必要なコンポーネントの一部を取得する必要があります
            // Here we get a reference to the default notary and instantiate a transaction builder.
            //ここでは、デフォルトの公証人への参照を取得し、トランザクションビルダーをインスタンス化します。
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder tb = new TransactionBuilder(notary);

            // 4. Construct a transfer command to be added to the transaction.
            List<PublicKey> listOfRequiredSigners = inputStateToTransfer.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList());
            listOfRequiredSigners.add(newLender.getOwningKey());

            Command<Transfer> command = new Command<>(
                    new Transfer(),
                    listOfRequiredSigners
            );

            // 5. Add the command to the transaction using the TransactionBuilder.
            // 5. TransactionBuilderを使用して、コマンドをトランザクションに追加します。
            tb.addCommand(command);

            // 6. Add input and output states to flow using the TransactionBuilder.
            // 6. TransactionBuilderを使用して、入力状態と出力状態をフローに追加します。
            tb.addInputState(inputStateAndRefToTransfer);
            tb.addOutputState(inputStateToTransfer.withNewLender(newLender), IOUContract.IOU_CONTRACT_ID);

            // 7. Ensure that this flow is being executed by the current lender.
            // 7.このフローが現在の貸し手によって実行されていることを確認します。
            if (!inputStateToTransfer.lender.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new IllegalArgumentException("This flow must be run by the current lender.");
            }

            // 8. Verify and sign the transaction
            // 8.トランザクションを検証して署名する
            tb.verify(getServiceHub());
            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(tb);

            // 9. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            // 9. CollectSignaturesFlowを使用して、他のCordaノードから必要な署名をすべて収集します
            List<FlowSession> sessions = new ArrayList<>();

            for (AbstractParty participant: inputStateToTransfer.getParticipants()) {
                Party partyToInitiateFlow = (Party) participant;
                if (!partyToInitiateFlow.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                    sessions.add(initiateFlow(partyToInitiateFlow));
                }
            }
            sessions.add(initiateFlow(newLender));
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, sessions));
            /* 10. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             *
             * 10. FinalityFlowの出力を返します。FinalityFlowは、検証のためにトランザクションを公証人に送信し、
             *    適切なノードのボールトに永続化させます。
             */     
            return subFlow(new FinalityFlow(fullySignedTransaction, sessions));
        }
    }


    /**
     * This is the flow which signs IOU settlements.
     *これは、IOU決裁に署名するフローです。
     *The signing is handled by the [SignTransactionFlow].
     *署名は[SignTransactionFlow]によって処理されます。
     */
    @InitiatedBy(IOUTransferFlow.InitiatorFlow.class)
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
                @NotNull
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
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

}
