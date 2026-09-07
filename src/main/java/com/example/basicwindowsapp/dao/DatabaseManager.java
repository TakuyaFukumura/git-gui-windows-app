package com.example.basicwindowsapp.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.logging.Logger;

/**
 * データベース接続・管理クラス
 *
 * <p>SQLiteデータベースへの接続とテーブルの初期化を管理します。
 * シングルトンパターンを使用してアプリケーション全体で共有されます。</p>
 *
 * @author basic-windows-app
 * @version 0.2.0
 * @since 0.2.0
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /**
     * データベースファイル名
     */
    private static final String DB_NAME = "basicwindowsapp.db";

    /**
     * ユーザーごとのアプリケーションデータディレクトリ
     */
    private static final Path DATA_DIRECTORY = Paths.get(
            System.getProperty("user.home"), ".basic-windows-app");

    /**
     * シングルトンインスタンス
     */
    private static DatabaseManager instance;

    /**
     * プライベートコンストラクタ（シングルトンパターン）
     */
    private DatabaseManager() {
    }

    /**
     * DatabaseManagerのインスタンスを取得します（シングルトンパターン）
     *
     * @return DatabaseManagerのインスタンス
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * データベース接続を取得します
     *
     * @return データベース接続
     * @throws SQLException データベース接続エラーが発生した場合
     */
    public Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            return DriverManager.getConnection("jdbc:sqlite:" + getDatabasePath());
        } catch (IOException e) {
            throw new SQLException("データベース保存先を作成できませんでした", e);
        }
    }

    /**
     * データベースとテーブルを初期化します
     *
     * <p>アプリケーション起動時に呼び出され、必要なテーブルを作成し、
     * デフォルトデータを挿入します。</p>
     *
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // messagesテーブルを作成（存在しない場合のみ）
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """;
            stmt.execute(createTableSQL);

            // デフォルトメッセージが存在するかチェック
            String checkDataSQL = "SELECT COUNT(*) FROM messages";
            try (ResultSet resultSet = stmt.executeQuery(checkDataSQL)) {
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    // デフォルトメッセージを挿入
                    insertDefaultMessage(conn);
                }
            }
        }
    }

    /**
     * デフォルトメッセージをデータベースに挿入します
     *
     * @param conn データベース接続
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private void insertDefaultMessage(Connection conn) throws SQLException {
        long currentTime = System.currentTimeMillis();
        String insertSQL = "INSERT INTO messages (text, created_at) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, "Hello World");
            pstmt.setLong(2, currentTime);
            pstmt.executeUpdate();
        }

        LOGGER.info("デフォルトメッセージを挿入しました: Hello World");
    }

    /**
     * データベースファイルが存在するかチェックします
     *
     * @return データベースファイルが存在する場合true
     */
    public boolean databaseExists() {
        return Files.exists(getDatabasePath());
    }

    /**
     * データベースファイルを削除します（テスト・開発用）
     *
     * @return 削除に成功した場合true
     */
    public boolean deleteDatabase() {
        try {
            Files.deleteIfExists(getDatabasePath());
            return true;
        } catch (IOException e) {
            LOGGER.log(java.util.logging.Level.WARNING,
                    "データベースの削除に失敗しました: " + getDatabasePath(), e);
            return false;
        }
    }

    /**
     * データベースファイルの保存先を取得します。
     *
     * @return データベースファイルのパス
     */
    public Path getDatabasePath() {
        return DATA_DIRECTORY.resolve(DB_NAME);
    }
}
