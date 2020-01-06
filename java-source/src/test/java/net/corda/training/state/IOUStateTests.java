package net.corda.training.state;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.finance.*;

import static net.corda.training.TestUtils.*;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Practical exercise instructions.
 *エクササイズのイントロダクション。
 * Uncomment the first unit test [hasIOUAmountFieldOfCorrectType()] then run the unit test using the green arrow
 *最初の単体テスト[hasIOUAmountFieldOfCorrectType（）]のコメントを解除してから、緑色の矢印を使用して単体テストを実行します
 * to the left of the {@link IOUStateTests} class or the [hasIOUAmountFieldOfCorrectType()] method.
 *{@link IOUStateTests}クラスまたは[hasIOUAmountFieldOfCorrectType（）]メソッドの左側。
 * Running the unit tests from {@link IOUStateTests} runs all of the unit tests defined in the class.
 * {@link IOUStateTests}から単体テストを実行すると、クラスで定義されたすべての単体テストが実行されます。
 * The test should fail because you need to make some changes to the IOUState to make the test pass. Read the TODO
 * under each task number for a description and a hint of what you need to do.
 *テストに合格するには、IOUStateにいくつかの変更を加える必要があるため、テストは失敗するはずです。 
 *各タスク番号の下にあるTODOを読んで、説明と必要なヒントを確認してください。
 * Once you have the unit test passing, uncomment the next test.
 *単体テストに合格したら、次のテストのコメントを解除します。
 * Continue until all the unit tests pass.
 *すべてのユニットテストに合格するまで続行します。
 * Hint: CMD / Ctrl + click on the brown type names in square brackets for that type's definition in the codebase.
 *ヒント：CMD / Ctrl +コードベース内のそのタイプの定義の角括弧内の茶色のタイプ名をクリックします。
 */

public class IOUStateTests {

    /**
     * Task 1.
     * TODO: Add an 'amount' property of type {@link Amount} to the {@link IOUState} class to get this test to pass.
     * TODO：このテストに合格するには、{@ link Amount}タイプの 'amount'プロパティを{@link IOUState}クラスに追加します。
     * Hint: {@link Amount} is a template class that takes a class parameter of the token you would like an {@link Amount} of.
     *ヒント：{@link Amount}は、{@ link Amount}のトークンのクラスパラメーターを受け取るテンプレートクラスです。
     * As we are dealing with cash lent from one Party to another a sensible token to use would be {@link Currency}.
     *ある当事者から別の当事者に貸した現金を扱っているので、使用する賢明なトークンは{@link Currency}になります。
     */
    @Test
    public void hasIOUAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the amount field exist?
        //金額フィールドは存在しますか?
        Field amountField = IOUState.class.getDeclaredField("amount");
        // Is the amount field of the correct type?
        //金額フィールドは正しいタイプですか？
        assertTrue(amountField.getType().isAssignableFrom(Amount.class));
    }

    /**
     * Task 2.
     * TODO: Add a 'lender' property of type {@link Party} to the {@link IOUState} class to get this test to pass.
     * TODO：このテストに合格するには、タイプ{@link Party}の「lender」プロパティを{@link IOUState}クラスに追加します。
     */
    @Test
    public void hasLenderFieldOfCorrectType() throws NoSuchFieldException {
        // Does the lender field exist?
        //貸し手フィールドは存在しますか？
        Field lenderField = IOUState.class.getDeclaredField("lender");
        // Is the lender field of the correct type?
        //貸し手フィールドは正しいタイプですか？
        assertTrue(lenderField.getType().isAssignableFrom(Party.class));
    }

    /**
     * Task 3.
     * TODO: Add a 'borrower' property of type {@link Party} to the {@link IOUState} class to get this test to pass.
     * TODO：このテストに合格するには、{@ link Party}タイプの 'borrower'プロパティを{@link IOUState}クラスに追加します。
     */
    @Test
    public void hasBorrowerFieldOfCorrectType() throws NoSuchFieldException {
        // Does the borrower field exist?
        //借用者フィールドは存在しますか？
        Field borrowerField = IOUState.class.getDeclaredField("borrower");
        // Is the borrower field of the correct type?
        //借用者フィールドは正しいタイプですか？
        assertTrue(borrowerField.getType().isAssignableFrom(Party.class));
    }

    /**
     * Task 4.
     * TODO: Add a 'paid' property of type {@link Amount] to the {@link IOUState} class to get this test to pass.
     * TODO：このテストに合格するには、タイプ{@link Amount]の「paid」プロパティを{@link IOUState}クラスに追加します。
     * Hint:
     * - We would like this property to be initialised to a zero amount of Currency upon creation of the {@link IOUState}.
     *-{@link IOUState}の作成時に、このプロパティをゼロの量の通貨に初期化してください。
     * - You can use the {@link Currencies#POUNDS} function from Currencies to create an amount of pounds e.g. 'Currencies.POUNDS(10)'.
     *-通貨の{@link Currencies＃POUNDS}関数を使用して、ポンドを作成できます。 「Currencies.POUNDS（10）」。
     * - This property keeps track of how much of the initial [IOUState.amount] has been settled by the borrower
     *-このプロパティは、借り手によって初期[IOUState.amount]がどの程度決済されたかを追跡します
     *
     * - We need to make sure that the [IOUState.paid] property is of the same currency type as the
     *   [IOUState.amount] property. You can create an instance of the {@link Amount} class that takes a zero value and a token
     *   representing the currency - which should be the same currency as the [IOUState.amount] property.
     *-[IOUState.paid]プロパティが[IOUState.amount]プロパティと同じ通貨タイプであることを確認する必要があります。 
     *ゼロの値と通貨を表すトークンを受け取る{@link Amount}クラスのインスタンスを作成できます。
     *このトークンは[IOUState.amount]プロパティと同じ通貨である必要があります。
     */
    @Test
    public void hasPaidFieldOfCorrectType() throws NoSuchFieldException {
        // Does the paid field exist?
        //有料フィールドは存在しますか？
        Field paidField = IOUState.class.getDeclaredField("paid");
        // Is the paid field of the correct type?
        //有料フィールドは正しいタイプですか？
        assertTrue(paidField.getType().isAssignableFrom(Amount.class));
    }

    /**
     * Task 5.
     * TODO: Include the lender within the {@link IOUState#getParticipants()} list
     * TODO：{@link IOUState＃getParticipants（）}リスト内に貸し手を含める
     * Hint: [Arrays.asList()] takes any number of parameters and will add them to the list
     *ヒント：[Arrays.asList（）]は任意の数のパラメーターを取り、それらをリストに追加します
     */
    @Test
    public void lenderIsParticipant() {
        IOUState iouState = new IOUState(Currencies.POUNDS(0), ALICE.getParty(), BOB.getParty());
        assertNotEquals(iouState.getParticipants().indexOf(ALICE.getParty()), -1);
    }

    /**
     * Task 6.
     * TODO: Similar to the last task, include the borrower within the [IOUState.participants] list
     * TODO：最後のタスクと同様に、借用者を[IOUState.participants]リストに含めます
     */
    @Test
    public void borrowerIsParticipant() {
        IOUState iouState = new IOUState(Currencies.POUNDS(0), ALICE.getParty(), BOB.getParty());
        assertNotEquals(iouState.getParticipants().indexOf(BOB.getParty()), -1);
    }

    /**
     * Task 7.
     * TODO: Implement {@link LinearState} along with the required methods.
     * TODO：必要なメソッドとともに{@link LinearState}を実装します。
     * Hint: {@link LinearState} implements {@link ContractState} which defines an additional method. You can use
     * IntellIJ to automatically add the member definitions for you or you can add them yourself. Look at the definition
     * of {@link LinearState} for what requires adding.
     * ヒント：{@link LinearState}は、追加のメソッドを定義する{@link ContractState}を実装しています。 
     * 　　　　IntellIJを使用して、メンバー定義を自動的に追加することも、自分で追加することもできます。 
     *　　　　 追加が必要なものについては、{@ link LinearState}の定義をご覧ください。
     */
    @Test
    public void isLinearState() {
        assert(LinearState.class.isAssignableFrom(IOUState.class));
    }

    /**
     * Task 8.
     * TODO: Override the [LinearState.getLinearId()] method and have it return a value created via your state's constructor.
     * TODO：[LinearState.getLinearId（）]メソッドをオーバーライドし、状態のコンストラクターによって作成された値を返すようにします。
     * Hint:
     * - {@link LinearState#getLinearId} must return a [linearId] property of type {@link UniqueIdentifier}. You will need to create
     * a new instance field.
     *-{@link LinearState＃getLinearId}は、タイプ{@link UniqueIdentifier}の[linearId]プロパティを返す必要があります。 
     *新しいインスタンスフィールドを作成する必要があります。
     * - The [linearId] is designed to link all {@link LinearState}s (which represent the state of an
     * agreement at a specific point in time) together. All the {@link LinearState}s with the same [linearId]
     * represent the complete life-cycle to date of an agreement, asset or shared fact.
     *-[linearId]は、{@ link LinearState}（特定の時点での合意の状態を表す）をすべてリンクするように設計されています。 
     *同じ[linearId]を持つすべての{@link LinearState}は、契約、資産、または共有ファクトの現在までの完全なライフサイクルを表します。
     * - Create a new public constructor that creates an {@link IOUState} with a newly generated [linearId].
     *-新しく生成された[linearId]で{@link IOUState}を作成する新しいパブリックコンストラクターを作成します。
     * - Note: With two constructors, it must be specified which one is to be used by the serialization engine to generate
     * the class schema. The default constructor should be selected as it allows for recreation of all the fields. To
     * accomplish this, add an @ConstructorForDeserialization annotation to the default constructor.
     *-注：2つのコンストラクターでは、クラススキーマを生成するためにシリアル化エンジンが使用するコンストラクターを指定する必要があります。 
     *すべてのフィールドを再作成できるため、デフォルトのコンストラクターを選択する必要があります。 
     *これを行うには、@ ConstructorForDeserialization注釈をデフォルトのコンストラクターに追加します。
     */
    @Test
    public void hasLinearIdFieldOfCorrectType() throws NoSuchFieldException {
        // Does the linearId field exist?
        // linearIdフィールドは存在しますか？
        Field linearIdField = IOUState.class.getDeclaredField("linearId");

        // Is the linearId field of the correct type?
        // linearIdフィールドは正しいタイプですか？
        assertTrue(linearIdField.getType().isAssignableFrom(UniqueIdentifier.class));
    }

    /**
     * Task 9.
     * TODO: Ensure parameters are ordered correctly.
     * TODO：パラメーターの順序が正しいことを確認してください。
     * Hint: Make sure that the lender and borrower fields are not in the wrong order as this may cause some
     * confusion in subsequent tasks!
     *ヒント：貸し手と借り手のフィールドの順序が間違っていないことを確認してください。これにより、後続のタスクで混乱が生じる可能性があります。
     */
    @Test
    public void checkIOUStateParameterOrdering() throws NoSuchFieldException {

        List<Field> fields = Arrays.asList(IOUState.class.getDeclaredFields());

        int amountIdx = fields.indexOf(IOUState.class.getDeclaredField("amount"));
        int lenderIdx = fields.indexOf(IOUState.class.getDeclaredField("lender"));
        int borrowerIdx = fields.indexOf(IOUState.class.getDeclaredField("borrower"));
        int paidIdx = fields.indexOf(IOUState.class.getDeclaredField("paid"));
        int linearIdIdx = fields.indexOf(IOUState.class.getDeclaredField("linearId"));

        assertTrue(amountIdx < lenderIdx);
        assertTrue(lenderIdx < borrowerIdx);
        assertTrue(borrowerIdx < paidIdx);
        assertTrue(paidIdx < linearIdIdx);
    }

    /**
     * Task 10.
     * TODO: Add a helper method called [pay] that can be called from an {@link IOUState} to settle an amount of the IOU.
     *TODO：{@link IOUState}から呼び出すことができる[pay]というヘルパーメソッドを追加して、IOUの量を確定します。
     * Hint:
     * - You will need to increase the [IOUState.paid] property by the amount the borrower wishes to pay.
     *-借り手が支払う金額だけ[IOUState.paid]プロパティを増やす必要があります。
     * - Add a new function called [pay] in {@link IOUState}. This function will need to return an {@link IOUState}.
     *-{@link IOUState}に[pay]という新しい関数を追加します。 この関数は{@link IOUState}を返す必要があります。
     * - The existing state is immutable, so a new state must be created from the existing state. As this change represents
     * an update in the lifecycle of an asset, it should share the same [linearId]. To enforce this distinction between
     * updating vs creating a new state, make the default constructor private, to be used as a copy constructor.
     *-既存の状態は不変なので、既存の状態から新しい状態を作成する必要があります。 この変更はアセットのライフサイクルの更新を表すため、
     *同じ[linearId]を共有する必要があります。 更新と新しい状態の作成を区別するために、デフォルトのコンストラクターをプライベートにし、コピーコンストラクターとして使用します。
     */
    @Test
    public void checkPayHelperMethod() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        assertEquals(Currencies.DOLLARS(5), iou.pay(Currencies.DOLLARS(5)).getPaid());
        assertEquals(Currencies.DOLLARS(3), iou.pay(Currencies.DOLLARS(1)).pay(Currencies.DOLLARS(2)).getPaid());
        assertEquals(Currencies.DOLLARS(10), iou.pay(Currencies.DOLLARS(5)).pay(Currencies.DOLLARS(3)).pay(Currencies.DOLLARS(2)).getPaid());
    }

    /**
     * Task 11.* TODO: Add a helper method called [wit
      * TODO: Add a helper method called [withNewLender] that can be called from an {@link }IOUState} to change the IOU's lender.
      * TODO：{@link} IOUState}から呼び出してIOUの貸し手を変更できる[withNewLender]というヘルパーメソッドを追加します。
     * - This will also utilize the copy constructor.
     *-これはコピーコンストラクタも利用します。
     */
    @Test
    public void checkWithNewLenderHelperMethod() {
        IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
        assertEquals(MINICORP.getParty(), iou.withNewLender(MINICORP.getParty()).getLender());
        assertEquals(MEGACORP.getParty(), iou.withNewLender(MEGACORP.getParty()).getLender());
    }

    /**
     * Task 12.
     * TODO: Ensure constructors are overloaded correctly.
     * This test serves as a sanity check that the two constructors have been implemented properly. If it fails, refer to the instructions of Tasks 8 and 10.
     */
    @Test
    public void correctConstructorsExist() {
        // Public constructor for new states
        try {
            Constructor<IOUState> contructor = IOUState.class.getConstructor(Amount.class, Party.class, Party.class);
        } catch( NoSuchMethodException nsme ) {
            fail("The correct public constructor does not exist!");
        }
        // Private constructor for updating states
        try {
            Constructor<IOUState> contructor = IOUState.class.getDeclaredConstructor(Amount.class, Party.class, Party.class, Amount.class, UniqueIdentifier.class);
        } catch( NoSuchMethodException nsme ) {
            fail("The correct private copy constructor does not exist!");
        }
    }
}
