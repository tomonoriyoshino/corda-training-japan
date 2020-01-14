package net.corda.training.flow;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.*;
import net.corda.core.node.NodeInfo;
import net.corda.testing.node.*;
import net.corda.core.identity.Party;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.TransactionBuilder;


import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;

import java.util.stream.Collectors;
import java.util.concurrent.Future;
import java.util.*;

import org.junit.*;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.core.IsInstanceOf.*;

import java.security.PublicKey;

/**
 * Practical exercise instructions Flows part 1.
 *実践的な演習の手順フローパート1。
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 *ユニットテストのコメントを外し、ヒント+ユニットテスト本体を使用して、ユニットテストがパスするようにFLowを完了します。
 */
public class IOUIssueFlowTests {

    private MockNetwork mockNetwork;
    private StartedMockNode a, b;

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

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUIssueFlow.ResponderFlow.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Task 1.
     * Build out the {@link IOUIssueFlow}!
     * {@link IOUIssueFlow}を作成します！
     * TODO: Implement the {@link IOUIssueFlow} flow which builds and returns a partially {@link SignedTransaction}.
     * TODO：部分的に{@link SignedTransaction}を構築して返す{@link IOUIssueFlow}フローを実装します。
     * Hint:
     * - There's a lot to do to get this unit test to pass!
     *-この単体テストに合格するには、やることがたくさんあります！
     * - Create a {@link TransactionBuilder} and pass it a notary reference.
     *-{@link TransactionBuilder}を作成し、公証人の参照を渡します。
     * -- A notary {@link Party} object can be obtained from [FlowLogic.getServiceHub().getNetworkMapCache().getNotaryIdentities()].
     * -- [FlowLogic.getServiceHub（）.getNetworkMapCache（）.getNotaryIdentities（）]から公証人{@link Party}オブジェクトを取得できます。
     * -- In this training project there is only one notary
     *--このトレーニングプロジェクトには、公証人が1人しかいない
     * - Create a new {@link Command} object with the [IOUContract.Commands.Issue] type
     *-[IOUContract.Commandommands.Issue]タイプで新しい{@link Command}オブジェクトを作成します
     *-- The required signers will be the same as the state's participants
     *-必要な署名者はステートの参加者と同じになります
     * -- Add the {@link Command} to the transaction builder [addCommand].
     *-{@link Command}をトランザクションビルダー[addCommand]に追加します。
     * - Use the flow's {@link IOUState} parameter as the output state with [addOutputState]
     *-フローの{@link IOUState}パラメーターを[addOutputState]の出力状態として使用する
     * - Extra credit: use [TransactionBuilder.withItems] to create the transaction instead
     *-追加クレジット：[TransactionBuilder.withItems]を使用してトランザクションを作成します
     * - Sign the transaction and convert it to a {@link SignedTransaction} using the [getServiceHub().signInitialTransaction] method.
     *-[getServiceHub（）。signInitialTransaction]メソッドを使用して、トランザクションに署名し、{@ link SignedTransaction}に変換します。
     * - Return the {@link SignedTransaction}.
     *-{@link SignedTransaction}を返します。
     */
    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();

        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
        // IOUIssueFlowからunsigned（！）SignedTransactionオブジェクトを返します。
        SignedTransaction ptx = future.get();

        // Print the transaction for debugging purposes.
        //デバッグ目的でトランザクションを出力します。
        System.out.println(ptx.getTx());

        // Check the transaction is well formed...
        //トランザクションが正しい形式であることを確認します...
        // No outputs, one input IOUState and a command with the right properties.
        //出力なし、1つの入力IOUStateおよび適切なプロパティを持つコマンド。
        assert (ptx.getTx().getInputs().isEmpty());
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof IOUState);

        Command command = ptx.getTx().getCommands().get(0);
        assert (command.getValue() instanceof IOUContract.Commands.Issue);
        assert (new HashSet<>(command.getSigners()).equals(
                new HashSet<>(iou.getParticipants()
                        .stream().map(el -> el.getOwningKey())
                        .collect(Collectors.toList()))));

        ptx.verifySignaturesExcept(borrower.getOwningKey(),
                mockNetwork.getDefaultNotaryNode().getInfo().getLegalIdentitiesAndCerts().get(0).getOwningKey());
    }

    /**
     * Task 2.
     * Now we have a well formed transaction, we need to properly verify it using the {@link IOUContract}.
     *これで、整形式のトランザクションができました。{@ link IOUContract}を使用して適切に検証する必要があります。
     * TODO: Amend the {@link IOUIssueFlow} to verify the transaction as well as sign it.
     * TODO：{@link IOUIssueFlow}を修正して、トランザクションの検証と署名を行います。
     * Hint: You can verify on the builder directly prior to finalizing the transaction. This way
     * you can confirm the transaction prior to making it immutable with the signature.
     * ヒント：トランザクションを完了する前に、ビルダーで直接確認できます。 
     * これにより、署名で不変にする前にトランザクションを確認できます。
     */
    @Test
    public void flowReturnsVerifiedPartiallySignedTransaction() throws Exception {
        // Check that a zero amount IOU fails.
        //金額がゼロのIOUが失敗することを確認します。
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        IOUState zeroIou = new IOUState(Currencies.POUNDS(0), lender, borrower);
        Future<SignedTransaction> futureOne = a.startFlow(new IOUIssueFlow.InitiatorFlow(zeroIou));
        mockNetwork.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));

        futureOne.get();

        // Check that an IOU with the same participants fails.
        //同じ参加者のIOUが失敗することを確認します。
        IOUState borrowerIsLenderIou = new IOUState(Currencies.POUNDS(10), lender, lender);
        Future<SignedTransaction> futureTwo = a.startFlow(new IOUIssueFlow.InitiatorFlow(borrowerIsLenderIou));
        mockNetwork.runNetwork();
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        futureTwo.get();

        // Check a good IOU passes.
        //良好なIOUパスを確認します。
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        Future<SignedTransaction> futureThree = a.startFlow(new IOUIssueFlow.InitiatorFlow(iou));
        mockNetwork.runNetwork();
        futureThree.get();
    }

    /**
     * IMPORTANT: Review the {@link CollectSignaturesFlow} before continuing here.
     *重要：ここに進む前に{@link CollectSignaturesFlow}を確認してください。
     * Task 3.
     * Now we need to collect the signature from the [otherParty] using the {@link CollectSignaturesFlow}.
     *次に、{@ link CollectSignaturesFlow}を使用して[otherParty]から署名を収集する必要があります。
     * TODO: Amend the {@link IOUIssueFlow} to collect the [otherParty]'s signature.
     * TODO：[otherParty]の署名を収集するために{@link IOUIssueFlow}を修正します。
     * Hint:
     * On the Initiator side:
     *イニシエーター側：
     * - Get a set of the required signers from the participants who are not the node - refer to Task 6 of IOUIssueTests
     * - - [getOurIdentity()] will give you the identity of the node you are operating as
     * - Use [initateFlow] to get a set of {@link FlowSession} objects
     * - - Using [state.participants] as a base to determine the sessions needed is recommended. [participants] is on
     * - - the state interface so it is guaranteed to to exist where [lender] and [borrower] are not.
     *-ノードではない参加者から必要な署名者のセットを取得します-IOUIssueTestsのタスク6 [getOurIdentity（）]を参照して、
     *操作中のノードのIDを取得します[initateFlow]を使用して {@link FlowSession}オブジェクト必要なセッションを決定するベースとして
     *[state.participants]を使用することをお勧めします。 [参加者]は状態インターフェイス上にあるため、
     *[貸し手]と[借り手]が存在しない場所に存在することが保証されています。
     * - Use [subFlow] to start the {@link CollectSignaturesFlow}
     *-[subFlow]を使用して{@link CollectSignaturesFlow}を開始します
     * - Pass it a {@link SignedTransaction} object and {@link FlowSession} set
     *-{@link SignedTransaction}オブジェクトと{@link FlowSession}セットを渡します
     * - It will return a {@link SignedTransaction} with all the required signatures
     *-すべての必要な署名を含む{@link SignedTransaction}を返します
     * - The subflow performs the signature checking and transaction verification for you
     *-サブフローは署名チェックとトランザクション検証を実行します
     * <p>
     * On the Responder side:
     *レスポンダー側：
     * - Create a subclass of {@link SignTransactionFlow}
     *-{@link SignTransactionFlow}のサブクラスを作成します
     * - Override [SignTransactionFlow.checkTransaction] to impose any constraints on the transaction
     *-[SignTransactionFlow.checkTransaction]をオーバーライドして、トランザクションに制約を課します
     * <p>
     * Using this flow you abstract away all the back-and-forth communication required for parties to sign a
     * transaction.
     *このフローを使用すると、当事者がトランザクションに署名するために必要なすべてのやり取りを抽象化できます。
     */
    @Test
    public void flowReturnsTransactionSignedByBothParties() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();

        SignedTransaction stx = future.get();
        stx.verifyRequiredSignatures();
    }

    /**
     * Task 4.
     * Now we need to store the finished {@link SignedTransaction} in both counter-party vaults.
     *次に、完成した{@link SignedTransaction}を両方のカウンターパーティボールトに保存する必要があります。
     * TODO: Amend the {@link IOUIssueFlow} by adding a call to {@link FinalityFlow}.
     * TODO：{@link FinalityFlow}への呼び出しを追加して、{@ link IOUIssueFlow}を修正します。
     * Hint:
     * - As mentioned above, use the {@link FinalityFlow} to ensure the transaction is recorded in both {@link Party} vaults.
     *-上記のように、{@ link FinalityFlow}を使用して、トランザクションが両方の{@link Party}ボールトに記録されるようにします。
     * - Do not use the [BroadcastTransactionFlow]!
     *-[BroadcastTransactionFlow]を使用しないでください！
     * - The {@link FinalityFlow} determines if the transaction requires notarisation or not.
     *-{@link FinalityFlow}は、トランザクションに公証が必要かどうかを決定します。
     * - We don't need the notary's signature as this is an issuance transaction without a timestamp. There are no
     * inputs in the transaction that could be double spent! If we added a timestamp to this transaction then we
     * would require the notary's signature as notaries act as a timestamping authority.
     *-これはタイムスタンプのない発行トランザクションであるため、公証人の署名は必要ありません。 トランザクションには、
     *二重に使用される可能性のある入力はありません！ このトランザクションにタイムスタンプを追加した場合、
     *公証人はタイムスタンプ機関として機能するため、公証人の署名が必要になります。
     */
    @Test
    public void flowRecordsTheSameTransactionInBothPartyVaults() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();
        SignedTransaction stx = future.get();
        System.out.printf("Signed transaction hash: %h\n", stx.getId());

        Arrays.asList(a, b).stream().map(el ->
                el.getServices().getValidatedTransactions().getTransaction(stx.getId())
        ).forEach(el -> {
            SecureHash txHash = el.getId();
            System.out.printf("$txHash == %h\n", stx.getId());
            assertEquals(stx.getId(), txHash);
        });
    }
}
