package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import java.util.List;
import java.util.stream.Collectors;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.utilities.ProgressTracker;

import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;
import static net.corda.training.contract.IOUContract.Commands.*;

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 *これは、レジャーの新しいIOUの発行を処理するフローです。
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * 取引相手の署名の収集は[CollectSignaturesFlow]によって処理されます。
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * 公証（必要な場合）および元帳へのコミットメントは、[FinalityFlow]によって処理されます。
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 *フローは、レジャーにコミットされた[SignedTransaction]を返します。
 */
public class IOUIssueFlow {

    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final IOUState state;
        public InitiatorFlow(IOUState state) {
            this.state = state;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Step 1. Get a reference to the notary service on our network and our key pair.
            //ステップ1.ネットワーク上の公証サービスとキーペアへの参照を取得します。
            // Note: ongoing work to support multiple notary identities is still in progress.
            //注：複数の公証人IDをサポートするための進行中の作業はまだ進行中です。
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Step 2. Create a new issue command.
            //ステップ2.新しいissueコマンドを作成します。
            // Remember that a command is a CommandData object and a list of CompositeKeysct and a list of CompositeKeys
            //コマンドはCommandDataオブジェクトであり、CompositeKeysctのリストとCompositeKeysのリストであることを忘れないでください
            final Command<Issue> issueCommand = new Command<>(
                    new Issue(), state.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList()));

            // Step 3. Create a new TransactionBuilder object.
            //ステップ3.新しいTransactionBuilderオブジェクトを作成します。
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            //ステップ4. iouを出力状態として追加し、コマンドをトランザクションビルダーに追加します。
            builder.addOutputState(state, IOUContract.IOU_CONTRACT_ID);
            builder.addCommand(issueCommand);


            // Step 5. Verify and sign it with our KeyPair.
            //ステップ5. KeyPairで確認して署名します。
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);


            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            //ステップ6. SignTransactionFlowを使用して、相手の署名を収集します。
            List<Party> otherParties = state.getParticipants()
                    .stream().map(el -> (Party)el)
                    .collect(Collectors.toList());

            otherParties.remove(getOurIdentity());

            List<FlowSession> sessions = otherParties
                    .stream().map(el -> initiateFlow(el))
                    .collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    /**
     * This is the flow which signs IOU issuances.
     *これは、IOUの発行に署名するフローです。
     * The signing is handled by the [SignTransactionFlow].
     *署名は[SignTransactionFlow]によって処理されます。
     */
    @InitiatedBy(IOUIssueFlow.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {

        private final FlowSession flowSession;
        private SecureHash txWeJustSigned;

        public ResponderFlow(FlowSession flowSession){
            this.flowSession = flowSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends SignTransactionFlow {

                private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
                    super(flowSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(req -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        req.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    //トランザクションが検証されたら、txWeJustSignedID変数を初期化します。
                    txWeJustSigned = stx.getId();
                }
            }

            flowSession.getCounterpartyFlowInfo().getFlowVersion();

            // Create a sign transaction flow
            //署名トランザクションフローを作成します
            SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flow to sign the transaction
            //署名トランザクションフローを実行して、トランザクションに署名します
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            // ReceiveFinalityFlowを実行してトランザクションを終了し、ボールトに永続化します。
            return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));

        }
    }
}
