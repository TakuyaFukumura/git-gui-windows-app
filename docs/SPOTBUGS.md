# SpotBugs指摘一覧

## 概要

SpotBugs導入時の初回解析で、既存コードから中重要度の指摘が6件検出されました。
いずれも導入前から存在するコードに対する指摘であり、SpotBugsの導入自体による新規の問題ではありません。

現在のMaven設定では、SpotBugsを`verify`フェーズで実行し、高重要度以上の指摘がある場合にビルドを失敗させます。
今回の中重要度の指摘は、内容を記録したうえで別途対応する方針です。

## 指摘一覧

| # | 検出パターン | 該当箇所 | 概要 | 対応状況 |
|---|---|---|---|---|
| 1 | `EI_EXPOSE_REP2` | `GitGuiWindowsApp.java:84` | `Stage`をフィールドへ直接保持しているため、外部から変更可能なオブジェクトを格納していると検出された | 対応済み（JavaFXのライフサイクルに必要なため理由付き抑制） |
| 2 | `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | `ApplicationSettings.java:138` | `Path.getParent()`の戻り値が`null`になる可能性を考慮せず、`Files.createDirectories()`へ渡している | 対応済み（親パスを検証して`IOException`を返却） |
| 3 | `EI_EXPOSE_REP` | `ChangesPane.java:75` | 内部で保持しているJavaFXの`SplitPane`を`view()`から直接返している | 対応済み（画面構築に必要なため理由付き抑制） |
| 4 | `EI_EXPOSE_REP` | `CommitActionBar.java:40` | 内部で保持しているJavaFXの`HBox`を`view()`から直接返している | 対応済み（画面構築に必要なため理由付き抑制） |
| 5 | `EI_EXPOSE_REP` | `HistoryPane.java:124` | 内部で保持しているJavaFXの`VBox`を`view()`から直接返している | 対応済み（画面構築に必要なため理由付き抑制） |
| 6 | `EI_EXPOSE_REP` | `RepositoryToolbar.java:75` | 内部で保持しているJavaFXの`HBox`を`view()`から直接返している | 対応済み（画面構築に必要なため理由付き抑制） |

## 対応方針

### `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`

SpotBugsのカプセル化に関する指摘です。JavaFXのUI部品は、画面へ組み込むために
呼び出し側へ返す必要があり、単純なコピーを返すことはできません。
そのため、`@SuppressFBWarnings`に理由を記載し、画面構築に必要な公開であることを明示します。

### `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`

`ApplicationSettings`の設定ファイルパスは絶対正規化してから親ディレクトリを検証します。
親が存在しない場合は作成対象を推測せず、理由を含む`IOException`を返します。

## 解析方法

通常の検証では、次のコマンドでSpotBugsを実行します。

```bash
mvn verify
```

SpotBugsのチェックだけを実行する場合は、コンパイル済みのクラスに対して次を実行します。

```bash
mvn spotbugs:check
```
