package net.corda.training.state;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.util.*;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.training.contract.IOUContract;

import javax.servlet.http.Part;

/**
 * The IOU State object, with the following properties:
 *次のプロパティを持つIOU Stateオブジェクト：
 * - [amount] The amount owed by the [borrower] to the [lender]
 *-[金額] [借り手]が[貸し手]に支払うべき金額
 * - [lender] The lending party.
 *-[貸し手]貸し手。
 * - [borrower] The borrowing party.
 *-[借入者]借入者。
 * - [contract] Holds a reference to the [IOUContract]
 *-[契約] [IOUContract]への参照を保持します
 * - [paid] Records how much of the [amount] has been paid.
 *-[支払い済み] [支払い済み]の金額を記録します。
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *-[linearId]すべてのLinearState状態で共有される一意のID。
 * the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *すべての関係者のVault。 メソッドは、1つの入力と1つの出力がトランザクションでIDを共有していることを確認する必要があります。
 * except at issuance/termination.
 *発行/終了時を除きます。
 */

@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState, LinearState {

    public final Amount<Currency> amount;
    public final Party lender;
    public final Party borrower;
    public final Amount<Currency> paid;
    private final UniqueIdentifier linearId;

    // Private constructor used only for copying a State object
    // Stateオブジェクトのコピーにのみ使用されるプライベートコンストラクター
    @ConstructorForDeserialization
    private IOUState(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid, UniqueIdentifier linearId){
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public IOUState(Amount<Currency> amount, Party lender, Party borrower) {
        this(amount, lender, borrower, new Amount<>(0, amount.getToken()), new UniqueIdentifier());
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public Amount<Currency> getPaid() {
        return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     *このメソッドは、有効なトランザクションでこの状態を「使用」できるノードのリストを返します。 この場合、貸し手または借り手。
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * IOUを決済および転送するためのトランザクションを構築するときの補助となるメソッド。
     * - [pay] adds an amount to the paid property. It does no validation.
     *-[pay]は、支払われたプロパティに金額を追加します。 検証は行われません。
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     *-[withNewLender]は、新しく指定された貸し手で現在の状態のコピーを作成します。 転送するときに使用します。
     * - [copy] creates a copy of the state using the internal copy constructor ensuring the LinearId is preserved.
     *-[copy]は、内部コピーコンストラクターを使用して状態のコピーを作成し、LinearIdが保持されるようにします。
     */
    public IOUState pay(Amount<Currency> amountToPay) {
        Amount<Currency> newAmountPaid = this.paid.plus(amountToPay);
        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

    public IOUState copy(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid) {
        return new IOUState(amount, lender, borrower, paid, this.getLinearId());
    }

}
