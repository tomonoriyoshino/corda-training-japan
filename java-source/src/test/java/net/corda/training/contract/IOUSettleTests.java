package net.corda.training.contract;

import net.corda.core.contracts.*;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.MockServices;
import net.corda.training.state.IOUState;
import org.junit.Test;

import java.util.Arrays;
import java.util.Currency;

import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.training.TestUtils.BOB;
import static net.corda.training.TestUtils.ALICE;
import static net.corda.training.TestUtils.CHARLIE;

/**
 * Practical exercise instructions for Contracts Part 3.
 *コントラクトパート3の実践的な演習手順。
 * The objective here is to write some contract code that verifies a transaction to settle an [IOUState].
 * ここでの目的は、トランザクションを検証して[IOUState]を決済する契約コードを記述することです。
 * Settling is more complicated than transferring and issuing as it requires you to use multiple state types in a
 * transaction.
 *トランザクションで複数の状態タイプを使用する必要があるため、決済は転送および発行よりも複雑です。
 * As with the [IOUIssueTests] and [IOUTransferTests] uncomment each unit test and run them one at a time. Use the body
 * of the tests and the task description to determine how to get the tests to pass.
 * [IOUIssueTests]および[IOUTransferTests]と同様に、各ユニットテストのコメントを外し、一度に1つずつ実行します。 
 * テストの本文とタスクの説明を使用して、テストに合格する方法を決定します。
 */

public class IOUSettleTests {

    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(Arrays.asList("net.corda.training", "net.corda.finance.contracts"));

    private Cash.State createCashState(AbstractParty owner, Amount<Currency> amount) {
        OpaqueBytes defaultBytes = new OpaqueBytes(new byte[1]);
        PartyAndReference partyAndReference = new PartyAndReference(owner, defaultBytes);
        return new Cash.State(partyAndReference, amount, owner);
    }

    /**
     * Task 1.
     * We need to add another case to deal with settling in the [IOUContract.verify] function.
     * [IOUContract.verify]関数での決済に対処する別のケースを追加する必要があります。
     * TODO: Add the [IOUContract.Commands.Settle] case to the verify function.
     * TODO：[IOUContract.Commands.Settle]ケースを検証機能に追加します。
     * Hint: You can leave the body empty for now.
     * ヒント：今のところ、本文を空のままにしておくことができます。
     */
    @Test
    public void mustIncludeSettleCommand() {
        IOUState iou = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), BOB.getParty());
        Cash.State inputCash = createCashState(BOB.getParty(), Currencies.POUNDS(5));
        OwnableState outputCash = inputCash.withNewOwner(ALICE.getParty()).getOwnableState();

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.POUNDS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                return tx.failsWith("Contract Verification Failed");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.POUNDS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(BOB.getPublicKey(), new Commands.DummyCommand());
                return tx.failsWith("Contract verification failed");
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.POUNDS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                return tx.verifies();
            });
            return null;
        });

    }

    /**
     * Task 2.
     * For now, we only want to settle one IOU at once. We can use the [TransactionForContract.groupStates] function
     * to group the IOUs by their [linearId] property. We want to make sure there is only one group of input and output
     * IOUs.
     *現時点では、一度に1つのIOUのみを決済します。 [TransactionForContract.groupStates]関数を使用して、
     *[linearId]プロパティによってIOUをグループ化できます。 入力および出力IOUのグループが1つだけであることを確認する必要があります。
     * TODO: Using [groupStates] add a constraint that checks for one group of input/output IOUs.
     * TODO：[groupStates]を使用して、入出力IOUの1つのグループをチェックする制約を追加します。
     * Hint:
     * - The [groupStates] method on a Transaction takes two type parameters: the type of the state you wish to group by and the type
     *   of the grouping key used (indicated by a method reference), in this case as you need to use the [linearId] and it is a [UniqueIdentifier].
     * -トランザクションの[groupStates]メソッドは、グループ化する状態のタイプと、使用するグループ化キーのタイプ（メソッド参照で示される）の2つのタイプパラメーターを取ります。
     *  この場合、 [linearId]そしてそれは[UniqueIdentifier]です。
     *
     *       tx.groupStates(State.class, State::getLinearId)
     *
     */
    @Test
    public void mustBeOneGroupOfIOUs() {
        IOUState iouONE = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), BOB.getParty());
        IOUState iouTWO = new IOUState(Currencies.POUNDS(5), ALICE.getParty(), BOB.getParty());
        Cash.State inputCash = createCashState(BOB.getParty(), Currencies.POUNDS(5));
        CommandAndState outputCash = inputCash.withNewOwner(ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iouONE);
                tx.input(IOUContract.IOU_CONTRACT_ID, iouTWO);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.output(IOUContract.IOU_CONTRACT_ID, iouONE.pay(Currencies.POUNDS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash.getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.failsWith("List has more than one element.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iouONE);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.output(IOUContract.IOU_CONTRACT_ID, iouONE.pay(Currencies.POUNDS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash.getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return null;
        });

    }

    /**
     * Task 3.
     * There always has to be one input IOU in a settle transaction but there might not be an output IOU.
     *決済トランザクションには常に1つの入力IOUが必要ですが、出力IOUはない場合があります。
     * TODO: Add a constraint to check there is always one input IOU.
     * TODO：常に1つの入力IOUがあることを確認する制約を追加します。
     */
    @Test
    public void mustHaveOneInputIOU() {

        IOUState iou = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), BOB.getParty());
        IOUState iouOne = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), BOB.getParty());
        Cash.State tenPounds = createCashState( BOB.getParty(), Currencies.POUNDS(10));
        Cash.State fivePounds = createCashState( BOB.getParty(), Currencies.POUNDS(5));

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.failsWith("There must be one input IOU.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fivePounds);
                tx.output(Cash.class.getName(), fivePounds.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iouOne);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.input(Cash.class.getName(), tenPounds);
                tx.output(Cash.class.getName(), tenPounds.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return  null;
        });

    }

    /**
     * Task 4.
     * Now we need to ensure that there are cash states present in the outputs list. The [IOUContract] doesn't care
     * about input cash as the validity of the cash transaction will be checked by the [Cash] contract. We do however
     * need to count how much cash is being used to settle and update our [IOUState] accordingly.
     * ここで、出力リストにキャッシュ状態が存在することを確認する必要があります。 [IOUContract]は、現金取引の有効性が[Cash]契約によってチェックされるため、
     * 投入現金を気にしません。 ただし、それに応じて[IOUState]を決済および更新するために使用されている現金の量を
     * カウントする必要があります。
     * TODO: Filter out the cash states from the list of outputs list and assign them to a constant.
     * TODO：出力リストからキャッシュ状態を除外し、それらを定数に割り当てます。
     * Hint:
     * - Use the [outputsOfType] extension function to filter the transaction's outputs by type, in this case [Cash.State].
     *-[outputsOfType]拡張機能を使用して、タイプ（この場合は[Cash.State]）でトランザクションの出力をフィルターします。
     */
    @Test
    public void mustBeCashOutputStatesPresent() {

        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State cash = createCashState(BOB.getParty(), Currencies.DOLLARS(5));
        CommandAndState cashPayment = cash.withNewOwner(ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("There must be output cash.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), cash);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });

    }

    /**
     * Task 5.
     * Not only to we need to check that [Cash] output states are present but we need to check that the payer is
     * correctly assigning the lender as the new owner of these states.
     * [Cash]出力状態が存在することを確認するだけでなく、
     * 支払人がこれらの状態の新しい所有者として貸し手を正しく割り当てていることを確認する必要があります。
     * TODO: Add a constraint to check that the lender is the new owner of at least some output cash.
     * TODO：制約を追加して、貸し手が少なくとも一部のアウトプットキャッシュの新しい所有者であることを確認します。
     * Hint:
     * - Not all of the cash may be assigned to the lender as some of the input cash may be sent back to the borrower as change.
     *-投入された現金の一部が変更として借り手に返送される可能性があるため、すべての現金が貸し手に割り当てられるわけではありません。
     * - We need to use the [Cash.State.getOwner()] method to check to see that it is the value of our public key.
     *-[Cash.State.getOwner（）]メソッドを使用して、公開キーの値であることを確認する必要があります。
     * - Use [filter] to filter over the list of cash states to get the ones which are being assigned to us.
     *-[filter]を使用して、キャッシュ状態のリストをフィルタリングし、割り当てられている状態を取得します。
     * - Once we have this filtered list, we can sum the cash being paid to us so we know how much is being settled.
     *-このフィルタリングされたリストを取得したら、支払われている現金を合計できるため、決済されている金額を知ることができます。
     */
    @Test
    public void mustBeCashOutputStatesWithRecipientAsOwner() {
        IOUState iou = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), BOB.getParty());
        Cash.State cash = createCashState(BOB.getParty(), Currencies.POUNDS(5));
        CommandAndState invalidCashPayment = cash.withNewOwner(CHARLIE.getParty());
        CommandAndState validCashPayment = cash.withNewOwner(ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), cash);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.POUNDS(5)));
                tx.output(Cash.class.getName(), invalidCashPayment.getOwnableState());
                tx.command(BOB.getPublicKey(), invalidCashPayment.getCommand());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("There must be output cash paid to the recipient.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), cash);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.POUNDS(5)));
                tx.output(Cash.class.getName(), validCashPayment.getOwnableState());
                tx.command(BOB.getPublicKey(), validCashPayment.getCommand());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });

    }

    /**
     * Task 6.
     * Now we need to sum the cash which is being assigned to the lender and compare this total against how much of the iou is
     * left to pay.
     *ここで、貸し手に割り当てられている現金を合計し、この合計をiouの残りの支払額と比較する必要があります。
     * TODO: Add a constraint that checks the lender cannot be paid more than the remaining IOU amount left to pay.
     * TODO：残っているIOUの金額を超えて貸し手に支払うことができないことをチェックする制約を追加します。
     * Hint:
     * - The remaining amount of the IOU is the amount less the paid property.
     * -IOUの残額は、支払われた財産を差し引いた額です。
     * - To sum a list of [Cash.State]s, collect all [Cash.States] assigned to the lender and use a reduce method or
     * = explicitly loop through the list to sum the individual states.
     *-[Cash.State]のリストを合計するには、貸し手に割り当てられているすべての[Cash.States]を収集し、reduceメソッドを使用するか、
     * リストを明示的にループして個々の状態を合計します。
     * - We can compare the amount left paid to the amount being paid to use, ensuring the amount being paid isn't too much.
     *-支払った金額と使用する金額を比較し、支払った金額が多すぎないことを確認できます。
     */
    @Test
    public void cashSettlementAmountMustBeLessThanRemainingIOUAmount() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State elevenDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(11));
        Cash.State tenDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(10));
        Cash.State fiveDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(5));

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), elevenDollars);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(11)));
                tx.output(Cash.class.getName(), elevenDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("The amount settled cannot be more than the amount outstanding.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), tenDollars);
                tx.output(Cash.class.getName(), tenDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    /**
     * Task 7.
     * Your Java implementation should handle this for you but it goes without saying that we should only be able to settle
     * in the currency that the IOU in denominated in.
     * Java実装でこれを処理する必要がありますが、言うまでもなく、IOUの通貨でしか決済できません。
     * TODO: You shouldn't have anything to do here but here are some tests just to make sure!
     * TODO：ここでは何もするべきではありませんが、念のためにいくつかのテストを示します！
     */
    @Test
    public void cashSettlementMustBeInTheCorrectCurrency() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State tenDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(10));
        Cash.State tenPounds = createCashState( BOB.getParty(), Currencies.POUNDS(10));

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), tenPounds);
                tx.output(Cash.class.getName(), tenPounds.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("Token mismatch: GBP vs USD");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), tenDollars);
                tx.output(Cash.class.getName(), tenDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    /**
     * Task 8.
     * If we fully settle the IOU, then we are done and thus don't require one on ledgerServices.ledger anymore. However, if we only
     * partially settle the IOU, then we want to keep the IOU on ledger with an amended [paid] property.
     * IOUを完全に解決したら、完了です。したがって、ledgerServices.ledgerにIOUは必要ありません。 ただし、IOUを部分的にしか決済しない場合は、
     * 修正された[有料]プロパティを使用してIOUを元帳に保持する必要があります。
     * TODO: Write a constraint that ensures the correct behaviour depending on the amount settled vs amount remaining.
     * TODO：決済金額と残り金額に応じて正しい動作を保証する制約を記述します。
     * Hint: You can use a simple if statement and compare the total amount paid vs amount left to settle.
     * ヒント：簡単なifステートメントを使用して、支払総額と決済するための残額を比較できます。
     */
    @Test
    public void mustOnlyHaveOutputIOUIfNotFullySettling() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State tenDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(10));
        Cash.State fiveDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(5));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("There must be one output IOU.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), tenDollars);
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(10)));
                tx.output(Cash.class.getName(), tenDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("There must be no output IOU as it has been fully settled.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), tenDollars);
                tx.output(Cash.class.getName(), tenDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    /**
     * Task 9.
     * We want to make sure that the only property of the IOU which changes when we settle, is the paid amount.
     * 決済時に変更されるIOUの唯一のプロパティが支払額であることを確認する必要があります。
     * TODO: Write a constraint to check only the paid property of the [IOUState] changes when settling.
     * TODO：整定時に[IOUState]の変更の有料プロパティのみをチェックする制約を記述します。
     */
    @Test
    public void onlyPaidPropertyMayChange() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State fiveDollars = createCashState( BOB.getParty(), Currencies.DOLLARS(5));

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                IOUState iouCopy = iou.copy(iou.amount, iou.lender, CHARLIE.getParty(), iou.paid).pay(Currencies.DOLLARS(5));
                tx.output(IOUContract.IOU_CONTRACT_ID, iouCopy);
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("The borrower may not change when settling.");
                return null;
            });

            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                IOUState iouCopy = iou.copy(Currencies.DOLLARS(0), iou.lender, CHARLIE.getParty(), iou.paid).pay(Currencies.DOLLARS(5));
                tx.output(IOUContract.IOU_CONTRACT_ID, iouCopy);
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("The amount may not change when settling.");
                return null;
            });

            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                IOUState iouCopy = iou.copy(iou.amount, CHARLIE.getParty(), iou.borrower, iou.paid).pay(Currencies.DOLLARS(5));
                tx.output(IOUContract.IOU_CONTRACT_ID, iouCopy);
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("The lender may not change when settling.");
                return null;
            });

            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fiveDollars);
                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(ALICE.getParty()).getOwnableState());
                IOUState iouCopy = iou.copy(iou.amount, iou.lender, iou.borrower, iou.paid).pay(Currencies.DOLLARS(5));
                tx.output(IOUContract.IOU_CONTRACT_ID, iouCopy);
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.verifies();
                return null;
            });

            return null;
        });

    }

    /**
     * Task 10.
     * Both the lender and the borrower must have signed an IOU issue transaction.
     *貸し手と借り手の両方がIOU発行トランザクションに署名している必要があります。
     * TODO: Add a constraint to the contract code that ensures this is the case.
     * TODO：これを確実にする制約を契約コードに追加します。
     */
    public void mustBeSignedByAllParticipants() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        Cash.State cash = createCashState(BOB.getParty(), Currencies.DOLLARS(5));
        CommandAndState cashPayment = cash.withNewOwner(ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(Cash.class.getName(), cash);
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(ALICE.getPublicKey(), CHARLIE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(Cash.class.getName(), cash);
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(BOB.getPublicKey(), new IOUContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(Cash.class.getName(), cash);
                tx.input(IOUContract.IOU_CONTRACT_ID, iou);
                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.command(BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(BOB.getPublicKey(), ALICE.getPublicKey()), new IOUContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
                return null;
            });
            return null;
        });

    }

}
