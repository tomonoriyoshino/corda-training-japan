![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training Solutions

This repo contains all the solutions for the practical exercises of the Corda two-day training course.

This repository is divided into two parts: Java Solutions, and Kotlin Solutions.

このレポには、Cordaの2日間のトレーニングコースの実践的な演習のためのすべてのソリューションが含まれています。

このリポジトリは、JavaソリューションとKotlinソリューションの2つのパートに分かれています。

# Setup

### Tools
* JDK 1.8 latest version
* IntelliJ latest version (2017.1 or newer)
* git

After installing the required tools, clone or download a zip of this repository, and place it in your desired
location.

必要なツールをインストールした後、このリポジトリのzipをクローンまたはダウンロードし、目的の場所に配置します。

### IntelliJ setup
* From the main menu, click `open` (not `import`!) then navigate to where you placed this repository.
*メインメニューから「開く」（「インポート」ではありません！）をクリックし、このリポジトリを配置した場所に移動します。

* Click `File->Project Structure`, and set the `Project SDK` to be the JDK you downloaded (by clicking `new` and
nagivating to where the JDK was installed). Click `Okay`.
*「ファイル」->「プロジェクト構造」をクリックし、「プロジェクトSDK」をダウンロードしたJDKに設定します（「新規」をクリックして、
JDKがインストールされた場所に移動します）。 「OK」をクリックします。

* Next, click `import` on the `Import Gradle Project` popup, leaving all options as they are.
*次に、「Gradleプロジェクトのインポート」ポップアップで「インポート」をクリックし、すべてのオプションをそのままにします。

* If you do not see the popup: Navigate back to `Project Structure->Modules`, clicking the `+ -> Import` button,
navigate to and select the repository folder, select `Gradle` from the next menu, and finally click `Okay`,
again leaving all options as they are.
*ポップアップが表示されない場合： `Project Structure-> Modules`に戻り、` +-> Import`ボタンをクリックして、
リポジトリフォルダに移動して選択し、次のメニューから「Gradle」を選択し、最後に「OK」をクリックします。
再びすべてのオプションをそのままにします。


### Running the tests
* Kotlin: Select `Kotlin - Unit tests` from the dropdown run configuration menu, and click the green play button.
* Kotlin：ドロップダウン実行構成メニューから「Kotlin-単体テスト」を選択し、緑色の再生ボタンをクリックします。

* Java: Select `Java - Unit tests` from the dropdown run configuration menu, and click the green play button.
* Java：ドロップダウン実行構成メニューから「Java-単体テスト」を選択し、緑色の再生ボタンをクリックします。

* Individual tests can be run by clicking the green arrow in the line number column next to each test.
*個々のテストは、各テストの横にある行番号列の緑色の矢印をクリックして実行できます。

* When running flow tests you must add the following to your run / debug configuration in the VM options field. This enables us to use
*フローテストを実行するときは、VMオプションフィールドの実行/デバッグ構成に以下を追加する必要があります。 これにより、

* Quasar - a library that provides high-performance, lightweight threads.
* Quasar-高性能で軽量のスレッドを提供するライブラリ。

* "-javaagent: /PATH_TO_FILE_FROM_ROOT_DIR/quasar.jar"
* "-javaagent：/PATH_TO_FILE_FROM_ROOT_DIR/quasar.jar"



# Solutions Files

### Kotlin
State:

* Template: `kotlin-source/src/test/kotlin/net/corda/training/state/IOUState.kt`
* Tests: `kotlin-source/src/main/kotlin/net/corda/training/state/IOUStateTests.kt`

Contract:

* Template: `kotlin-source/src/main/kotlin/net/corda/training/contract/IOUContract.kt`
* Issue Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUIssueTests.kt`
* Transfer Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUTransferTests.kt`
* Settle Tests: `kotlin-source/src/test/kotlin/net/corda/training/contract/IOUSettleTests.kt`

Flow:

* Issue Flow Solution: `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUIssueFlow.kt`
* Issue Flow tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUIssueFlowTests.kt`
* Transfer Flow Solution: `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUTransferFlow.kt`
* Transfer Flow Tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUTransferFlowTests.kt`
* Settle Flow Solution: `kotlin-source/src/main/kotlin/net/corda/training/flow/IOUSettleFlow.kt`
* Settle Flow tests: `kotlin-source/src/test/kotlin/net/corda/training/flow/IOUSettleFlowTests.kt`

The code in the following files was already added for you:

* `kotlin-source/src/main/kotlin/net/corda/training/plugin/IOUPlugin.kt`
* `kotlin-source/src/test/kotlin/net/corda/training/Main.kt`
* `kotling-source/src/main/kotlin/net/corda/training/plugin/IOUPlugin.kt`
* `kotling-source/src/main/java/kotlin/corda/training/flow/SelfIssueCashFlow.kt`


### Java
State:

* Solution: `java-source/src/main/java/net/corda/training/state/IOUState.java`
* Tests: `java-source/src/test/java/net/corda/training/state/IOUStateTests.java`

Contract:

* Solution: `java-source/src/main/java/net/corda/training/contract/IOUContract.java`
* Issue Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`
* Transfer Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`
* Settle Tests: `java-source/src/test/java/net/corda/training/contract/IOUIssueTests.java`

Flow:

* Issue Solution: `java-source/src/main/java/net/corda/training/flow/IOUIssueFlow.java`
* Issue tests: `java-source/src/test/java/net/corda/training/flow/IOUIssueFlowTests.java`
* Transfer Solution: `java-source/src/main/java/net/corda/training/flow/IOUTransferFlow.java`
* Transfer tests: `java-source/src/test/java/net/corda/training/flow/IOUTransferFlowTests.java`
* Settle Solution: `java-source/src/main/java/net/corda/training/flow/IOUSettleFlow.java`
* Settle tests: `java-source/src/test/java/net/corda/training/flow/IOUSettleFlowTests.java`

The code in the following files was already added for you:

* `java-source/src/main/java/net/corda/training/plugin/IOUPlugin.java`
* `java-source/src/test/java/net/corda/training/NodeDriver.java`
* `java-source/src/main/java/net/corda/training/plugin/IOUPlugin.java`
* `java-source/src/main/java/net/corda/training/flow/SelfIssueCashFlow.java`


# Running the CorDapp
Once your application passes all tests in `IOUStateTests`, `IOUIssueTests`, and `IOUIssueFlowTests`, you can run the application and
interact with it via a web browser. To run the finished application, you have two choices for each language: from the terminal, and from IntelliJ.
アプリケーションが「IOUStateTests」、「IOUIssueTests」、および「IOUIssueFlowTests」のすべてのテストに合格すると、アプリケーションを実行して
Webブラウザーを介して対話します。 完成したアプリケーションを実行するには、言語ごとに2つの選択肢があります。ターミナルからとIntelliJからです。



### Kotlin
* Terminal: Navigate to the root project folder and run `./gradlew kotlin-source:deployNodes`, followed by
`./kotlin-source/build/node/runnodes`
*ターミナル：ルートプロジェクトフォルダーに移動し、 `。/ gradlew kotlin-source：deployNodes`を実行してから、
`。/ kotlin-source / build / node / runnodes`

* IntelliJ: With the project open, select `Kotlin - Node driver` from the dropdown run configuration menu, and click
the green play button.
* IntelliJ：プロジェクトを開いた状態で、ドロップダウン実行構成メニューから「Kotlin-Node driver」を選択し、クリックします
緑の再生ボタン。

### Java
* Terminal: Navigate to the root project folder and run `./gradlew java-source:deployNodes`, followed by
`./java-source/build/node/runnodes`
*ターミナル：ルートプロジェクトフォルダーに移動し、 `。/ gradlew java-source：deployNodes`を実行し、続いて
`。/ java-source / build / node / runnodes`

* IntelliJ: With the project open, select `Java - NodeDriver` from the dropdown run configuration menu, and click
the green play button.
* IntelliJ：プロジェクトを開いた状態で、ドロップダウン実行構成メニューから「Java-NodeDriver」を選択し、クリックします
緑の再生ボタン。



### Interacting with the CorDapp
Once all the three nodes have started up (look for `Webserver started up in XXX sec` in the terminal or IntelliJ ), you can interact
with the app via a web browser.
3つのノードすべてが起動したら（ターミナルまたはIntelliJで「WebサーバーがXXX秒で起動しました」を探してください）、対話することができます
Webブラウザ経由でアプリを使用します。

* From a Node Driver configuration, look for `Starting webserver on address localhost:100XX` for the addresses.
*ノードドライバー構成から、アドレスの「Starting webserver on address localhost：100XX」を探します。

* From the terminal: Node A: `localhost:10009`, Node B: `localhost:10012`, Node C: `localhost:10015`.
*ターミナルから：ノードA： `localhost：10009`、ノードB：` localhost：10012`、ノードC： `localhost：10015`。

To access the front-end gui for each node, navigate to `localhost:XXXX/web/iou/`
各ノードのフロントエンドGUIにアクセスするには、「localhost：XXXX / web / iou /」に移動します

## Troubleshooting:
When running the flow tests, if you get a Quasar instrumention error then add:
フローテストの実行時に、Quasarインスツルメンテーションエラーが発生した場合は、次を追加します。

```-ea -javaagent:lib/quasar.jar```

to the VM args property in the default run configuration for JUnit in IntelliJ.
IntelliJのJUnitのデフォルトの実行構成のVM argsプロパティに追加します。
