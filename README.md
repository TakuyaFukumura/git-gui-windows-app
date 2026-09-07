# Git GUI Windows App

JavaFXとSQLiteを使用したメッセージ管理アプリケーションです。

## 概要

このプロジェクトは、JavaFXとSQLiteデータベースを使用してメッセージの表示・編集・削除・登録を行うGUIアプリケーションです。
Windowsアプリ開発における基本的なCRUD操作のベースとして使用することができます。

## 機能

- **メッセージ表示**: SQLiteデータベースから取得したメッセージを画面に表示
- **CRUD操作**: メッセージの新規作成・編集・削除・一覧表示
- **メッセージ管理**: TableViewを使用したメッセージ一覧と操作UI
- **デフォルト復旧**: データベースが空になった場合に`Hello World`を自動復旧
- **データ永続化**: SQLiteによるローカルデータベース管理
- **入力検証**: 空白のみのメッセージは登録・更新不可
- **メッセージ検索**: 一覧を本文で絞り込み
- **TableView編集・複数選択**: 本文のインライン編集、複数行削除、選択行のエクスポートに対応
- **データ可視化**: アプリ情報画面に日別メッセージ件数のグラフを表示
- **ファイル入出力**: ファイルメニューからメッセージ一覧をUTF-8のテキストまたはCSVでインポート・エクスポート
- **ドラッグ＆ドロップ**: CSVまたはテキストファイルを画面へドロップしてインポート
- **安全なエクスポート**: 既存ファイルへの上書き前に確認
- **応答性の維持**: ファイル入出力とインポート登録をバックグラウンドで実行
- **処理ステータス**: ファイル入出力中の状態と進捗インジケーターを表示
- **診断情報**: アプリ情報画面にバージョン、Java、データ保存先を表示
- **バックアップ・復元**: データベースと設定を`.bwa`ファイルへ保存・復元
- **ダークモード**: 画面上部右側の明暗アイコンでライトモードとダークモードを切替
- **テーマ設定の保存**: 再起動後も最後に選択したテーマを復元
- **画面設定の保存**: ウィンドウサイズと位置を再起動後に復元
- **画面タブ**: メッセージ管理画面とアプリ情報画面を切替
- **メニューバーと操作ボタン**: メッセージ操作をメニューと画面下部の操作ボタンから実行
- **キーボードショートカット**: `Ctrl+N`（新規作成）、`Ctrl+E`（編集）、`Delete`（削除）、
  `F5`（更新）、`Ctrl+F`（検索）、`Ctrl+I`（インポート）、`Ctrl+Shift+E`（エクスポート）、
  `Ctrl+Q`（終了）

## 特徴

- **実用的なアプリ構成**: データベース連携を含む実際のアプリケーション構造
- **拡張性**: 新しい機能を追加しやすい設計（DAO パターン使用）
- **詳細な日本語コメント**: 初学者向けのJavadocコメント
- **Maven対応**: 依存関係の管理とビルドが簡単

## 必要な環境

- **Java**: Java 24 以上
- **Maven**: 3.6.0 以上（Maven Wrapperを使用する場合は不要）
- **OS**: Windows 10/11（他のOSでも動作可能）

## セットアップ手順

### 1. リポジトリのクローン

```bash
git clone https://github.com/TakuyaFukumura/git-gui-windows-app.git
```

```bash
cd git-gui-windows-app
```

### GitHub Copilot用スキル

`.github/skills/` に、リポジトリの改修、PRマージ・リリース、スキル改善を支援する手順を用意しています。Java/Maven/JavaFXの構成と、このリポジトリのブランチ・検証・安全ルールに合わせて記載しています。

### 2. 依存関係のインストール

#### Maven Wrapperを使用する場合（推奨）

```bash
./mvnw clean install
```

#### 通常のMavenを使用する場合

```bash
mvn clean install
```

## 実行方法

### 方法1: Maven JavaFXプラグインを使用（推奨）

#### Maven Wrapperを使用する場合

```bash
./mvnw javafx:run
```

#### 通常のMavenを使用する場合

```bash
mvn javafx:run
```

### 方法2: Javaコマンドを直接使用

まずプロジェクトをコンパイルします：

#### Maven Wrapperを使用する場合

```bash
./mvnw clean compile
```

#### 通常のMavenを使用する場合

```bash
mvn clean compile
```

次に、JavaFXモジュールを指定してアプリケーションを実行します：

```bash
java --module-path "path/to/javafx/lib" --add-modules javafx.controls -cp "target/classes:path/to/sqlite-jdbc.jar" com.example.gitguiwindowsapp.GitGuiWindowsApp
```

**注意**: `path/to/javafx/lib`はJavaFX SDKのライブラリパス、
`path/to/sqlite-jdbc.jar`はMavenから取得したSQLite JDBC JARのパスに置き換えてください。
Windowsではクラスパスの区切り文字に`;`を使用し、macOS/Linuxでは`:`を使用します。
例えばWindowsでは次のように実行します：

```cmd
java --module-path "path/to/javafx/lib" --add-modules javafx.controls -cp "target/classes;path/to/sqlite-jdbc.jar" com.example.gitguiwindowsapp.GitGuiWindowsApp
```

### 方法3: JARファイルの作成

#### Maven Wrapperを使用する場合

```bash
./mvnw clean package
```

#### 通常のMavenを使用する場合

```bash
mvn clean package
```

`target`ディレクトリにJARファイルが作成されます。
標準のJARにはアプリケーションのメインマニフェスト属性がないため、
アプリケーションの起動には方法1の`javafx:run`を使用してください。

### 方法4: Windowsアプリケーションイメージの作成

JDKに含まれる`jpackage`を使用して、Javaランタイムと依存ライブラリを含む
自己完結型のWindowsアプリケーションイメージを作成できます。
Windows向けのパッケージはWindows環境で作成してください。

```cmd
mvnw.cmd clean package -Pjpackage
```

作成されたアプリケーションは`target\dist\GitGuiWindowsApp`に出力されます。
アプリケーションイメージ内の`GitGuiWindowsApp.exe`から起動できます。
MSIインストーラーが必要な場合は、`pom.xml`の`<type>APP_IMAGE</type>`を
`<type>MSI</type>`に変更し、WiX Toolsetをインストールしてください。
MSIではインストール先、スタートメニュー、ショートカットなどを設定できます。

インストール後のSQLiteデータベースは、実行ディレクトリではなく
ユーザーのホームディレクトリ配下の`.git-gui-windows-app`に保存されます。

## プロジェクト構造

```
git-gui-windows-app/
├── pom.xml                                    # Mavenビルド設定
├── README.md                                  # このファイル
├── .gitignore                                 # Git無視ファイル設定
├── mvnw                                       # Maven Wrapper実行スクリプト（Unix/Linux/Mac用）
├── mvnw.cmd                                   # Maven Wrapper実行スクリプト（Windows用）
├── .mvn/                                      # Maven Wrapper設定
│   └── wrapper/
│       └── maven-wrapper.properties          # Maven Wrapperプロパティ
└── src/
    └── main/
        ├── java/
        │   └── com/example/gitguiwindowsapp/
        │       ├── GitGuiWindowsApp.java        # JavaFX UIとイベント処理
        │       ├── io/
        │       │   ├── BackupService.java       # データバックアップ／復元
        │       │   └── MessageFileService.java  # テキスト／CSV入出力
        │       ├── model/
        │       │   └── Message.java            # メッセージエンティティ
        │       ├── validation/
        │       │   └── MessageValidator.java   # メッセージ入力検証
        │       └── dao/
        │           ├── DatabaseManager.java    # SQLite接続・初期化
        │           └── MessageDao.java         # メッセージCRUD
        └── resources/                          # リソースファイル用ディレクトリ
```

アプリケーションの起動時にユーザーのホームディレクトリ配下の
`.git-gui-windows-app\gitguiwindowsapp.db`が作成されます。このファイルはアプリケーションの再起動後も保持されます。
テーマとウィンドウサイズ・位置は同じディレクトリの
`.git-gui-windows-app\settings.properties`（Java Properties形式）に保存されます。

## Maven Wrapperについて

このプロジェクトはMaven Wrapperを使用しており、Maven本体をインストールしなくてもプロジェクトをビルド・実行できます。

### Maven Wrapperの利点

- **環境依存なし**: 特定のMavenバージョンに依存せず、プロジェクト固有のMavenバージョンを使用
- **簡単セットアップ**: Javaさえインストールされていれば、追加のセットアップなしでビルド可能
- **一貫した環境**: チーム開発において全員が同じMavenバージョンを使用可能

### Maven Wrapperの使用方法

#### Windows環境の場合

```cmd
mvnw.cmd clean install
mvnw.cmd javafx:run
```

#### Unix/Linux/Mac環境の場合

```bash
./mvnw clean install
./mvnw javafx:run
```

**注意**: 初回実行時には、指定されたMavenバージョンが自動的にダウンロードされるため、インターネット接続が必要です。

## 主要なファイルの説明

### GitGuiWindowsApp.java

メインのアプリケーションクラスです。JavaFXの`Application`クラスを継承し、以下の機能を提供します：

- **ウィンドウの作成**: 初期サイズ800x600、最小サイズ600x400のリサイズ可能なウィンドウ
- **現在のメッセージ表示**: 最新メッセージを上部に大きく表示
- **メッセージ一覧**: ID、本文、作成日時をTableViewに表示
- **操作UI**: 新規作成、編集、削除、更新の各操作に対応
- **テーマ切替**: メイン画面と各種ダイアログにテーマを適用

### DatabaseManager.java

SQLiteへの接続を管理し、`messages`テーブルを初回起動時に作成します。
テーブルが空の場合は、初期データとして`Hello World`を登録します。

### MessageDao.java

DAOパターンでデータアクセスを分離し、以下の操作を提供します：

- メッセージの登録、取得、更新、削除
- 最新メッセージの取得
- メッセージ件数の取得
- 全メッセージ削除後のデフォルトメッセージ復旧

### データベース設計

```sql
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
```

### pom.xml

Maven設定ファイルです。以下の設定が含まれています：

- **Java 24対応**: JavaFX 26.0.2を使用可能
- **JavaFX依存関係**: JavaFX Controlsライブラリ
- **SQLite JDBC**: SQLite 3.53.4.0によるローカルデータ永続化
- **プラグイン設定**: コンパイルと実行用の設定

## 開発ガイド

### 新機能の追加

1. **新しいクラスの追加**: `src/main/java/com/example/gitguiwindowsapp/`ディレクトリに新しいJavaファイルを作成
2. **リソースファイルの追加**: `src/main/resources/`ディレクトリにFXMLファイルや画像などを配置
3. **依存関係の追加**: 必要に応じて`pom.xml`に新しい依存関係を追加

## トラブルシューティング

### JavaFXランタイムが見つからない場合

Java 11以降では、JavaFXはJDKから分離されています。以下の対処法があります：

1. **JavaFX SDKのダウンロード**: [OpenJFX公式サイト](https://openjfx.io/)からダウンロード
2. **環境変数の設定**: `PATH_TO_FX`環境変数にJavaFXライブラリパスを設定
3. **IDEの設定**: IntelliJ IDEAやEclipseでJavaFXライブラリを設定

### ビルドエラーが発生する場合

#### Maven Wrapperを使用する場合

```bash
./mvnw clean
```

```bash
./mvnw compile
```

#### 通常のMavenを使用する場合

```bash
mvn clean
```

```bash
mvn compile
```

で依存関係をクリアしてから再ビルドしてください。

## CI/CD設定

このプロジェクトはGitHub Actionsを使用してCI/CDを自動化しています。

### 自動化された処理

- **ビルドテスト**: プッシュやプルリクエスト時に自動でコンパイルテストを実行
- **クロスプラットフォームテスト**: Ubuntu、Windows、macOSでのビルド確認
- **依存関係のキャッシュ**: Mavenの依存関係をキャッシュして高速化

### ワークフロー詳細

詳細な設定は `.github/workflows/ci.yml` ファイルをご確認ください。
