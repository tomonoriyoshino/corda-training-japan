package net.corda.training.flow;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.finance.schemas.CashSchemaV1;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.*;
import net.corda.training.contract.IOUContract;
import net.corda.training.contract.IOUIssueTests;
import net.corda.training.state.IOUState;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Signed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static net.corda.testing.driver.Driver.driver;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.training.TestUtils.ALICE;
import static net.corda.training.TestUtils.BOB;

/**
 * Practical exercise instructions Flows part 3.
 *実習の手順フローパート3。
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 *ユニットテストのコメントを外し、ヒント+ユニットテスト本体を使用して、ユニットテストがパスするようにFLowを完了します。
 */
public class IOUSettleFlowTests{

    private MockNetwork mockNetwork;
    private StartedMockNode a, b, c;

    @Before
    public void setup() {

        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(
                Arrays.asList(
                        TestCordapp.findCordapp("net.corda.training"),
                        TestCordapp.findCordapp("net.corda.finance.schemas")
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
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUSettleFlow.Responder.class));
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

    private Cash.State issueCash(Amount<Currency> amount) throws InterruptedException, ExecutionException {
        SelfIssueCashFlow flow = new SelfIssueCashFlow(amount);
        CordaFuture future = a.startFlow(flow);
        mockNetwork.runNetwork();
        return (Cash.State) future.get();
    }

    /**
     * Task 1.
     * The first task is to grab the [IOUState] for the given [linearId] from the vault, assemble a transaction
     * and sign it.
     *最初のタスクは、指定された[linearId]の[IOUState]をボールトから取得し、トランザクションをアセンブルして署名することです。
     * TODO: Grab the IOU for the given [linearId] from the vault, build and sign the settle transaction.
     * TODO：ボールトから特定の[linearId]のIOUを取得し、決済トランザクションをビルドして署名します。
     * Hints:
     * - Use the code from the [IOUTransferFlow] to get the correct [IOUState] from the vault.
     *-[IOUTransferFlow]のコードを使用して、ボールトから正しい[IOUState]を取得します。
     * - You will need to use the [Cash.generateSpend] functionality of the vault to add the cash states and cash command
     *   to your transaction. The API is quite simple. It takes a reference to a [ServiceHub], [TransactionBuilder], an [Amount],
     *   our Identity as a [PartyAndCertificate], the [Party] object for the recipient, and a set of the spending parties.
     *-キャッシュ状態とキャッシュコマンドをトランザクションに追加するには、ボールトの[Cash.generateSpend]機能を使用する必要があります。 
     *APIは非常に単純です。 [ServiceHub]、[TransactionBuilder]、[Amount]、[PartyAndCertificate]としてのアイデンティティ、
     *受信者の[Party]オブジェクト、および支出パーティのセットへの参照を取ります。
     *   The function will mutate your builder by adding the states and commands.
     *この関数は、ステートとコマンドを追加することにより、ビルダーを変更します。
     * - You then need to produce the output [IOUState] by using the [IOUState.pay] function.
     *-[IOUState.pay]関数を使用して、出力[IOUState]を生成する必要があります。
     * - Add the input [IOUState] [StateAndRef] and the new output [IOUState] to the transaction.
     *-入力[IOUState] [StateAndRef]および新しい出力[IOUState]をトランザクションに追加します。
     * - Sign the transaction and return it.
     *-トランザクションに署名して返します。
     */
    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = a.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction settleResult = futureSettleResult.get();
        // Check the transaction is well formed...
        //トランザクションが正しい形式であることを確認します...
        // One output IOUState, one input IOUState reference, input and output cash
        // 1つの出力IOUState、1つの入力IOUState参照、入力および出力キャッシュ
        a.transaction(() -> {
            try {
                LedgerTransaction ledgerTx = settleResult.toLedgerTransaction(a.getServices(), false);
                assert(ledgerTx.getInputs().size() == 2);
                assert(ledgerTx.getOutputs().size() == 2);

                IOUState outputIOU = ledgerTx.outputsOfType(IOUState.class).get(0);
                IOUState correctOutputIOU = inputIOU.pay(Currencies.POUNDS(5));

                assert (outputIOU.amount.equals(correctOutputIOU.amount));
                assert (outputIOU.paid.equals(correctOutputIOU.paid));
                assert (outputIOU.lender.equals(correctOutputIOU.lender));
                assert (outputIOU.borrower.equals(correctOutputIOU.borrower));

                // Sum all the output cash. This is complicated as there may be multiple cash output states with not all of them
                // being assigned to the lender.
                //すべての出力現金を合計します。 これは複雑です。複数の現金出力状態があり、
                //それらのすべてが貸し手に割り当てられているわけではないからです。
                List<Cash.State> outputCash = ledgerTx.getOutputs().stream()
                        .map(state -> (Cash.State) state.getData())
                        .filter(state -> state.getOwner().getOwningKey().equals(b.getInfo().getLegalIdentities().get(0).getOwningKey()))
                        .collect(Collectors.toList());

                // Sum the acceptable cash sent to the lender
                //貸し手に送られた許容現金を合計します
                Amount<Currency> outputCashSum = new Amount<>(0, inputIOU.amount.getToken());
                for (Cash.State cash: outputCash) {
                    Amount<Currency> addCash = new Amount<>(cash.getAmount().getQuantity(), cash.getAmount().getToken().getProduct());
                    outputCashSum = outputCashSum.plus(addCash);
                }

                assert (outputCashSum.equals(inputIOU.amount.minus(inputIOU.paid).minus(outputIOU.paid)));

                CommandWithParties command = ledgerTx.getCommands().get(0);
                assert (command.getValue().equals(new IOUContract.Commands.Settle()));

                settleResult.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey(),
                        mockNetwork.getDefaultNotaryIdentity().getOwningKey());

                return null;
            } catch (Exception exception) {
                System.out.println(exception);
            }
            return null;
        });
    }

    /**
     * Task 2.
     * Only the borrower should be running this flow for a particular IOU.
     * 特定のIOUに対してこのフローを実行しているのは借り手だけです。
     * TODO: Grab the IOU for the given [linearId] from the vault and check the node running the flow is the borrower.
     * TODO：ボールトから特定の[linearId]のIOUを取得し、フローを実行しているノードが借用者であることを確認します。
     * Hint: Use the data within the iou obtained from the vault to check the right node is running the flow.
     *ヒント：ボールトから取得したiou内のデータを使用して、適切なノードがフローを実行していることを確認します。
     */
    @Test
    public void settleFlowCanOnlyBeRunByBorrower() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = b.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            futureSettleResult.get();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: The borrower must issue the flow");
        }
    }

    /**
     * Task 3.
     * The borrower must have at least SOME cash in the right currency to pay the lender.
     *借り手は、貸し手に支払うために少なくとも適切な通貨でいくらかの現金を持っていなければなりません。
     * TODO: Add a check in the flow to ensure that the borrower has a balance of cash in the right currency.
     * TODO：フローにチェックを追加して、借り手が適切な通貨の現金残高を持っていることを確認します。
     * Hint:
     * - Use [getCashBalance(getServiceHub(), (Currency) amount.getToken())].
     *-[getCashBalance（getServiceHub（）,（Currency）amount.getToken（））]を使用します。
     * - Use an if statement to check there is cash in the right currency present.
     *-if文を使用して、適切な通貨の現金が存在することを確認します。
     */
    @Test
    public void borrowerMustHaveCashInRightCurrency() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = a.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            futureSettleResult.get();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: Borrower has no GBP to settle.");
        }
    }

    /**
     * Task 4.
     * The borrower must have enough cash in the right currency to pay the lender.
     *借り手は、貸し手に支払うために適切な通貨で十分な現金を持っている必要があります。
     * TODO: Add a check in the flow to ensure that the borrower has enough cash to pay the lender.
     * TODO：フローにチェックを追加して、借り手が貸し手を支払うのに十分な現金を持っていることを確認します。
     * Hint: Add another if statement similar to the one required above.
     *ヒント：上記で必要なものと同様の別のifステートメントを追加します。
     */
    @Test
    public void borrowerMustHaveEnoughCashInRightCurrency() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = a.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            futureSettleResult.get();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: Borrower doesn't have enough cash to settle with the amount specified.");
        }
    }

    /**
     * Task 5.
     * We need to get the transaction signed by the other party.
     *取引相手の署名を取得する必要があります。
     * TODO: Use a subFlow call to [initiateFlow] and the [SignTransactionFlow] to get a signature from the lender.
     * TODO：[initiateFlow]および[SignTransactionFlow]へのsubFlow呼び出しを使用して、貸し手から署名を取得します。
     */
    @Test
    public void flowReturnsTransactionSignedByBothParties() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = a.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            futureSettleResult.get().verifySignaturesExcept(mockNetwork.getDefaultNotaryIdentity().getOwningKey());
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: Borrower has no GBP to settle.");
        }
    }

    /**
     * Task 6.
     * We need to get the transaction signed by the notary service
     *公証サービスによって署名されたトランザクションを取得する必要があります
     * TODO: Use a subFlow call to the [FinalityFlow] to get a signature from the lender.
     * TODO：[FinalityFlow]へのsubFlow呼び出しを使用して、貸し手から署名を取得します。
     */
    @Test
    public void flowReturnsCommittedTransaction() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        Future<SignedTransaction> futureSettleResult = a.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            futureSettleResult.get().verifyRequiredSignatures();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: Borrower has no GBP to settle.");
        }
    }

}
