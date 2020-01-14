package net.corda.training.flow;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.Currencies;
import net.corda.testing.node.*;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class IOUTransferFlowTests {

    private MockNetwork mockNetwork;
    private StartedMockNode a, b, c;

    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(
                Arrays.asList(
                        TestCordapp.findCordapp("net.corda.training")
                )
        ).withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"))));
        mockNetwork = new MockNetwork(mockNetworkParameters);
        System.out.println(mockNetwork);

        a = mockNetwork.createNode(new MockNodeParameters());
        b = mockNetwork.createNode(new MockNodeParameters());
        c = mockNetwork.createNode(new MockNodeParameters());

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);
        startedNodes.add(c);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        //実際のノードの場合、これは自動的に行われますが、テスト用のフローを手動で登録する必要があります
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUTransferFlow.Responder.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private SignedTransaction issueIOU(IOUState iouState) throws InterruptedException, ExecutionException {
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iouState);
        CordaFuture future = a.startFlow(flow);
        mockNetwork.runNetwork();
        return (SignedTransaction) future.get();
    }

    /**
     * Task 1.
     * Build out the beginnings of [IOUTransferFlow]!
     * [IOUTransferFlow]の始まりを構築しましょう！
     * TODO: Implement the [IOUTransferFlow] flow which builds and returns a partially [SignedTransaction].
     * TODO：部分的に[SignedTransaction]を構築して返す[IOUTransferFlow]フローを実装します。
     * Hint:
     * - This flow will look similar to the [IOUIssueFlow].
     * -このフローは[IOUIssueFlow]に似ています。
     * - This time our transaction has an input state, so we need to retrieve it from the vault!
     *-今回はトランザクションに入力状態があるため、ボールトから取得する必要があります！
     * - You can use the [getServiceHub().getVaultService().queryBy(Class, queryCriteria)] method to get the latest linear states of a particular
     *-[getServiceHub（）.getVaultService（）.queryBy（Class、queryCriteria）]メソッドを使用して、特定の最新の線形状態を取得できます。
     *   type from the vault. It returns a list of states matching your query.
     *ボールトから入力します。 クエリに一致する状態のリストを返します。
     * - Use the [UniqueIdentifier] which is passed into the flow to create the appropriate Query Criteria.
     *-フローに渡される[UniqueIdentifier]を使用して、適切なクエリ条件を作成します。
     * - Use the [IOUState.withNewLender] method to create a copy of the state with a new lender.
     *-[IOUState.withNewLender]メソッドを使用して、新しい貸し手で状態のコピーを作成します。
     * - Create a Command - we will need to use the Transfer command.
     *-コマンドを作成する-Transferコマンドを使用する必要があります。
     * - Remember, as we are involving three parties we will need to collect three signatures, so need to add three
     *   [PublicKey]s to the Command's signers list. We can get the signers from the input IOU and the new IOU you
     *   have just created with the new lender.
     *-3つの関係者が関与しているため、3つの署名を収集する必要があるため、コマンドの署名者リストに3つの[PublicKey]を
     *追加する必要があることを忘れないでください。 入力IOUおよび新しい貸し手で作成した新しいIOUから署名者を取得できます。
     * - Verify and sign the transaction as you did with the [IOUIssueFlow].
     *-[IOUIssueFlow]で行ったように、トランザクションを検証して署名します。
     * - Return the partially signed transaction.
     *-部分的に署名されたトランザクションを返します。
     */
    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().getLegalIdentities().get(0));
        Future<SignedTransaction> future = a.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction ptx = future.get();

        // Check the transaction is well formed...
        //トランザクションが正しい形式であることを確認します...
        // One output IOUState, one input state reference and a Transfer command with the right properties.
        // 1つの出力IOUState、1つの入力状態参照、および適切なプロパティを持つTransferコマンド。
        assert (ptx.getTx().getInputs().size() == 1);
        assert (ptx.getTx().getOutputs().size() == 1);
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof IOUState);
        assert (ptx.getTx().getInputs().get(0).equals(new StateRef(stx.getId(), 0)));

        IOUState outputIOU = (IOUState) ptx.getTx().getOutput(0);
        Command command = ptx.getTx().getCommands().get(0);

        assert (command.getValue().equals(new IOUContract.Commands.Transfer()));
        ptx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey(), c.getInfo().getLegalIdentities().get(0).getOwningKey(), mockNetwork.getDefaultNotaryIdentity().getOwningKey());
    }

    /**
     * Task 2.
     * We need to make sure that only the current lender can execute this flow.
     *現在の貸し手のみがこのフローを実行できることを確認する必要があります。
     * TODO: Amend the [IOUTransferFlow] to only allow the current lender to execute the flow.
     * TODO：[IOUTransferFlow]を修正して、現在の貸し手のみがフローを実行できるようにします。
     * Hint:
     * - Remember: You can use the node's identity and compare it to the [Party] object within the [IOUState] you
     *   retrieved from the vault.
     * -要確認：ノードのIDを使用して、ボールトから取得した[IOUState]内の[Party]オブジェクトと比較できます。
     * - Throw an [IllegalArgumentException] if the wrong party attempts to run the flow!
     *-間違ったパーティがフローを実行しようとした場合、[IllegalArgumentException]をスローします！
     */
    @Test
    public void flowCanOnlyBeRunByCurrentLender() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = b.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: This flow must be run by the current lender.");
        }
    }

    /**
     * Task 3.
     * Check that an [IOUState] cannot be transferred to the same lender.
     * [IOUState]を同じ貸主に譲渡できないことを確認します。
     * TODO: You shouldn't have to do anything additional to get this test to pass. Belts and Braces!
     * TODO：このテストに合格するために追加の作業を行う必要はありません。 ベルトとブレース！
     */
    @Test
    public void iouCannotBeTransferredToSameParty() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            assert exception.getMessage().equals("Contract verification failed: Failed requirement: The lender property must change in a transfer.");
        }
    }

    /**
     * Task 4.
     * Get the borrowers and the new lenders signatures.
     * 借り手と新しい貸し手の署名を取得します。
     * TODO: Amend the [IOUTransferFlow] to handle collecting signatures from multiple parties.
     * TODO：[IOUTransferFlow]を修正して、複数の関係者からの署名の収集を処理します。
     * Hint: use [initiateFlow] and the [CollectSignaturesFlow] in the same way you did for the [IOUIssueFlow].
     *ヒント：[IOUIssueFlow]の場合と同じ方法で[initiateFlow]および[CollectSignaturesFlow]を使用します。
     */
    @Test
    public void flowReturnsTransactionSignedBtAllParties() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), lender);
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
            stx.verifySignaturesExcept(mockNetwork.getDefaultNotaryIdentity().getOwningKey());
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Task 5.
     * We need to get the transaction signed by the notary service
     *公証サービスによって署名されたトランザクションを取得する必要があります
     * TODO: Use a subFlow call to the [FinalityFlow] to get a signature from the lender.
     * TODO：[FinalityFlow]へのsubFlow呼び出しを使用して、貸し手から署名を取得します。
     */
    @Test
    public void flowReturnsTransactionSignedByAllPartiesAndNotary() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
            stx.verifyRequiredSignatures();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }
}
