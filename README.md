![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Training Solutions

このレポには、Cordaの2日間のトレーニングコースの実践的な演習のためのすべてのソリューションが含まれています
このリポジトリは、JavaソリューションとKotlinソリューションの2つのパートに分かれています。

# Setup

### Tools
* JDK 1.8 latest version
* IntelliJ latest version (2017.1 or newer)
* git

必要なツールをインストールした後、このリポジトリのzipをクローンまたはダウンロードし、目的の場所に配置します。

### IntelliJ setup
*メインメニューから「開く」（「インポート」ではありません！）をクリックし、このリポジトリを配置した場所に移動します。

*「ファイル」->「Project Stracture」をクリックし、「Project SDK」をダウンロードしたJDKに設定します（「新規」をクリックして、
JDKがインストールされた場所に移動します）。 「OK」をクリックします。

*次に、「Gradleプロジェクトのインポート」ポップアップで「インポート」をクリックし、すべてのオプションをそのままにします。

*ポップアップが表示されない場合： `Project Structure-> Modules`に戻り、` +-> Import`ボタンをクリックして、
リポジトリフォルダに移動して選択し、次のメニューから「Gradle」を選択し、最後に「OK」をクリックします。
再びすべてのオプションをそのままにします。


### Running the tests
* Kotlin：ドロップダウン実行構成メニューから「Kotlin-単体テスト」を選択し、緑色の再生ボタンをクリックします。

* Java：ドロップダウン実行構成メニューから「Java-単体テスト」を選択し、緑色の再生ボタンをクリックします。

*個々のテストは、各テストの横にある行番号列の緑色の矢印をクリックして実行できます。

*フローテストを実行するときは、VMオプションフィールドの実行/デバッグ構成に以下を追加する必要があります。 これにより、

* Quasar-高性能で軽量のスレッドを提供するライブラリ。

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

アプリケーションが「IOUStateTests」、「IOUIssueTests」、および「IOUIssueFlowTests」のすべてのテストに合格すると、アプリケーションを実行して
Webブラウザーを介して対話します。 完成したアプリケーションを実行するには、言語ごとに2つの選択肢があります。ターミナルからとIntelliJからです。



### Kotlin
*ターミナル：ルートプロジェクトフォルダーに移動し、 `。/ gradlew kotlin-source：deployNodes`を実行してから、
`。/ kotlin-source / build / node / runnodes`

* IntelliJ：プロジェクトを開いた状態で、ドロップダウン実行構成メニューから「Kotlin-Node driver」を選択し、クリックします
緑の再生ボタン。

### Java
*ターミナル：ルートプロジェクトフォルダーに移動し、 `。/ gradlew java-source：deployNodes`を実行し、続いて
`。/ java-source / build / node / runnodes`

* IntelliJ：プロジェクトを開いた状態で、ドロップダウン実行構成メニューから「Java-NodeDriver」を選択し、クリックします
緑の再生ボタン。


### Interacting with the CorDapp
3つのノードすべてが起動したら（ターミナルまたはIntelliJで「WebサーバーがXXX秒で起動しました」を探してください）、対話することができます
Webブラウザ経由でアプリを使用します。

*ノードドライバー構成から、アドレスの「Starting webserver on address localhost：100XX」を探します。

*ターミナルから：ノードA： `localhost：10009`、ノードB：` localhost：10012`、ノードC： `localhost：10015`。

各ノードのフロントエンドGUIにアクセスするには、「localhost：XXXX / web / iou /」に移動します

## Troubleshooting:
フローテストの実行時に、Quasarインスツルメンテーションエラーが発生した場合は、次を追加します。

```-ea -javaagent:lib/quasar.jar```

IntelliJのJUnitのデフォルトの実行構成のVM argsプロパティに追加します。
