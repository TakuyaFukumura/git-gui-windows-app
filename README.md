# Git GUI Windows App

JavaFX でローカル Git リポジトリを確認・操作するデスクトップアプリケーションです。
メッセージ管理や SQLite は使用しません。

## 現在の機能

- リポジトリの選択と Git リポジトリ検証
- `git status --porcelain=v1 -z` によるブランチ・変更ファイル表示
- unified diff の表示（ステージ済み／未ステージ、バイナリ・10 MB超過の案内）
- ファイル単位のステージ、アンステージ、コミット
- ローカルブランチの一覧、作成、切り替え、マージ済みブランチの安全な削除
- 未コミット変更を保持したブランチ切り替え時の確認、Git準拠のブランチ名検証
- Git 実行ファイル、開くダイアログの基準フォルダー、最近使用したリポジトリ（最大5件）の保存

Git コマンドはシェルを介さず `ProcessBuilder` の引数リストで実行します。
実行基盤、status/diff パーサー、サービス層は JavaFX UI から分離されています。

## 必要な環境

- Java 24 以上
- Maven 3.6 以上（Maven Wrapper を使用可能）
- Git 2.30 以上（PATH に配置、または設定で実行ファイルを指定）
- Windows 10/11（JavaFX が動作する macOS/Linux でも利用可能）

## ビルドとテスト

Windows:

```cmd
mvnw.cmd clean install
```

通常の Maven を使う場合:

```bash
mvn clean install
```

## 起動

```cmd
mvnw.cmd javafx:run
```

初回起動後、「開く...」からリポジトリのフォルダーを選択してください。
前回開いたリポジトリが存在する場合は自動的に再接続します。
「基準フォルダー...」から「開く...」の初期フォルダーを指定できます。

## 設定

設定は `%USERPROFILE%\.git-gui-windows-app\settings.properties` に保存されます。
Git 実行ファイル、「開く...」の基準フォルダー、最近のリポジトリ、ウィンドウ位置・サイズを保持します。

## プロジェクト構成

```text
src/main/java/com/example/gitguiwindowsapp/
  GitGuiWindowsApp.java       JavaFX のリポジトリ/status UI
  config/ApplicationSettings.java
  git/                         runner、parser、GitService
  model/                       Git の不変モデル
src/test/java/.../git/         runner、parser、service の単体テスト
docs/実装計画書.md              次の実装計画
```

## 現在の対象外

リモート操作、マージ・コンフリクト解決、部分ステージ、stash、タグ、
高度な履歴表示、配布用インストーラーは今後のフェーズです。
