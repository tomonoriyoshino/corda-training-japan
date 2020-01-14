package net.corda.training.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.MockServices;
import net.corda.training.state.IOUState;
import org.junit.Test;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.training.TestUtils.*;

/**
 * Practical exercise instructions for Contracts Part 2.
 *契約パート2の実践的な演習手順。
 * The objective here is to write some contract code that verifies a transaction to issue an [IOUState].
 *ここでの目的は、トランザクションを検証して[IOUState]を発行する契約コードを記述することです。
 * As with the [IOUIssueTests] uncomment each unit test and run them one at a time. Use the body of the tests and the
 * task description to determine how to get the tests to pass.
 * [IOUIssueTests]と同様に、各ユニットテストのコメントを外し、一度に1つずつ実行します。 
 * テストの本文とタスクの説明を使用して、テストに合格する方法を決定します。
 */

public class IOUTransferTests {

    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(Arrays.asList("net.corda.training", "net.corda.finance.contracts"));

    // A dummy state
    IOUState dummyState = new IOUState(Currencies.DOLLARS(0), CHARLIE.getParty(), CHARLIE.getParty());

    // function to create new Cash states.
    private Cash.State createCashState(AbstractParty owner, Amount<Currency> amount) {
        OpaqueBytes defaultBytes = new OpaqueBytes(new byte[1]);
        PartyAndReference partyAndReference = new PartyAndReference(owner, defaultBytes);
        return new Cash.State(partyAndReference, amount, owner);
    }


    /**
     * Task 1.
     * Now things are going to get interesting!
     * 今、物事は面白くなりそうです！
     * We need the [IOUContract] to not only handle Issues of IOUs but now also Transfers.
     * [IOUContract]は、IOUの発行の問題だけでなく、転送も処理する必要があります。
     * Of course, we'll need to add a new Command and add some additional contract code to handle Transfers.
     *もちろん、新しいコマンドを追加し、転送を処理するためにいくつかの追加の契約コードを追加する必要があります。
     * TODO: Add a "Transfer" command to the IOUState and update the verify() function to handle multiple commands.
     * TODO：「Transfer」コマンドをIOUStateに追加し、verify（）関数を更新して複数のコマンドを処理します。
     * Hint:
     * - As with the [Issue] command, add the [Transfer] command within the [IOUContract.Commands].
     *-[Issue]コマンドと同様に、[IOUContract.Commands]内に[Transfer]コマンドを追加します。
     * - Again, we only care about the existence of the [Transfer] command in a transaction, therefore it should
     *   subclass the [TypeOnlyCommandData].
     *-繰り返しますが、トランザクション内の[Transfer]コマンドの存在のみを考慮しているため、
     * [TypeOnlyCommandData]をサブクラス化する必要があります。
     * - You can use the [requireSingleCommand] function to check for the existence of a command which implements a
     *   specified interface:
     *-[requireSingleCommand]関数を使用して、指定したインターフェイスを実装するコマンドの存在を確認できます。
     *
     *       final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
     *       final Commands commandData = command.getValue();
     *
     *   To match any command that implements [IOUContract.Commands]
     * [IOUContract.Commands]を実装するコマンドに一致させるため
     * - We then need conditional logic based on the type of [Command.value], in Java you can do this using an "if-else" statement
     *-[Command.value]のタイプに基づく条件付きロジックが必要です。Javaでは、「if-else」ステートメントを使用してこれを行うことができます
     * - For each "if", or "elseIf" block, you can check the type of [Command.value]:
     *-「if」または「elseIf」ブロックごとに、[Command.value]のタイプを確認できます。
     *
     *        if (commandData.equals(new Commands.Issue())) {
     *        requireThat(require -> {...})
     *        } else if (...) {}
     *
     * - The [requireSingleCommand] function will handle unrecognised types for you (see first unit test).
     *-[requireSingleCommand]関数は、認識されないタイプを処理します（最初のユニットテストを参照）。
     */
    @Test
    public void mustHandleMultipleCommandValues() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new Commands.DummyCommand());
                return tx.failsWith("Required net.corda.training.contract.IOUContract.Commands command");
            });
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.withNewLender(CHARLIE.getParty()));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 2.
     * The transfer transaction should only have one input state and one output state.
     *転送トランザクションは、1つの入力状態と1つの出力状態のみを持つ必要があります。
     * TODO: Add constraints to the contract code to ensure a transfer transaction has only one input and output state.
     * TODO：コントラクトコードに制約を追加して、転送トランザクションの入力状態と出力状態が1つだけであることを確認します。
     * Hint:
     * - Look at the contract code for "Issue".
     *-「問題」の契約コードを見てください。
     */
    @Test
    public void mustHaveOneInputAndOneOutput() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(IOUContract.IOU_CONTRACT_ID, dummyState);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.withNewLender(CHARLIE.getParty()));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("An IOU transfer transaction should only consume one input state.");
            });
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.withNewLender(CHARLIE.getParty()));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("An IOU transfer transaction should only consume one input state.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("An IOU transfer transaction should only create one output state.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.withNewLender(CHARLIE.getParty()));
                tx.output(IOUContract.IOU_CONTRACT_ID, dummyState);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith(" An IOU transfer transaction should only create one output state.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.withNewLender(CHARLIE.getParty()));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()),new IOUContract.Commands.Transfer());
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 3.
     * TODO: Add a constraint to the contract code to ensure only the lender property can change when transferring IOUs.
     * TODO：契約コードに制約を追加して、IOUの転送時に貸し手プロパティのみが変更できるようにします。
     * Hint:
     * - You should create a private internal copy constructor, accessible via a copy method on your IOUState.
     *-IOUStateのcopyメソッドを介してアクセス可能なプライベート内部コピーコンストラクターを作成する必要があります。
     * - You can then compare a copy of the input to the output with the lender of the output as the lender of the input.
     *-次に、入力のコピーを出力と比較することで出力の貸し手と入力の貸し手を比較できます。
     * - You'll need references to the input and output ious.
     *-入力と出力への参照が必要です。
     * - Remember you need to cast the [ContractState]s to [IOUState]s.
     *-[ContractState]を[IOUState]にキャストする必要があることを忘れないでください。
     * - It's easier to take this approach then check all properties other than the lender haven't changed, including
     *   the [linearId] and the [contract]!
     *-[linearId]や[contract]を含め、貸し手が変更されていない以外のすべてのプロパティをチェックしてから、このアプローチを採用する方が簡単です。
     */
    @Test
    public void onlyTheLenderMayChange() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(1), ALICE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("Only the lender property may change.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), ALICE.getParty(), CHARLIE.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("Only the lender property may change.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty(), Currencies.DOLLARS(5)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("Only the lender property may change.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.verifies();
            });
            return null;
        });
    }


    /**
     * Task 4.
     * It is fairly obvious that in a transfer IOU transaction the lender must change.
     *転送IOUトランザクションでは、貸し手は変更する必要があることはかなり明白です。
     * TODO: Add a constraint to check the lender has changed in the output IOU.
     * TODO：出力IOUで貸し手が変更されたことを確認する制約を追加します。
     */
    @Test
    public void theLenderMustChange() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The lender property must change in a transfer.");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 5.
     * All the participants in a transfer IOU transaction must sign.
     *転送IOUトランザクションのすべての参加者は署名する必要があります。
     * TODO: Add a constraint to check the old lender, the new lender and the recipient have signed.
     * TODO：制約を追加して、古い貸し手、新しい貸し手、および受信者が署名したことを確認します。
     */
    @Test
    public void allParticipantsMustSign() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(CHARLIE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), MINICORP.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey(), MINICORP.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.failsWith("The borrower, old lender and new lender only must sign an IOU transfer transaction");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.copy(Currencies.DOLLARS(10), CHARLIE.getParty(), BOB.getParty(), Currencies.DOLLARS(0)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Transfer());
                return tx.verifies();
            });
            return null;
        });
    }

}
