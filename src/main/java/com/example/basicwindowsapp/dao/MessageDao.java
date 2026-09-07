package com.example.basicwindowsapp.dao;

import com.example.basicwindowsapp.model.Message;
import com.example.basicwindowsapp.validation.MessageValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * メッセージDAO（Data Access Object）クラス
 *
 * <p>メッセージエンティティに対するCRUD操作を提供します。
 * データベースとのやり取りを一元化し、ビジネスロジックから
 * データアクセス詳細を分離します。</p>
 *
 * @author basic-windows-app
 * @version 0.2.0
 * @since 0.2.0
 */
public class MessageDao {

    private static final Logger LOGGER = Logger.getLogger(MessageDao.class.getName());
    /**
     * デフォルトメッセージ（削除時の復旧用）
     */
    private static final String DEFAULT_MESSAGE = "Hello World";
    /**
     * DatabaseManagerのインスタンス
     */
    private final DatabaseManager dbManager;

    /**
     * コンストラクタ
     */
    public MessageDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * 新しいメッセージを挿入します
     *
     * @param message 挿入するメッセージオブジェクト
     * @return 挿入されたメッセージのID
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int insertMessage(Message message) throws SQLException {
        String text = MessageValidator.normalize(message.getText());
        String sql = "INSERT INTO messages (text, created_at) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, text);
            pstmt.setLong(2, message.getCreatedAt());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("メッセージの挿入に失敗しました。");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("メッセージの挿入に失敗しました。IDが生成されませんでした。");
                }
            }

        }
    }

    /**
     * メッセージを1つのトランザクションでまとめて挿入します。
     *
     * @param messages 挿入するメッセージ
     * @return 挿入件数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int insertMessages(List<Message> messages) throws SQLException {
        String sql = "INSERT INTO messages (text, created_at) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < messages.size(); i++) {
                    Message message = messages.get(i);
                    try {
                        pstmt.setString(1, MessageValidator.normalize(message.getText()));
                        pstmt.setLong(2, message.getCreatedAt());
                        pstmt.executeUpdate();
                    } catch (RuntimeException | SQLException e) {
                        conn.rollback();
                        throw new SQLException("インポートの" + (i + 1) + "件目に失敗しました。", e);
                    }
                }
                conn.commit();
                return messages.size();
            } catch (SQLException e) {
                if (!conn.isClosed()) {
                    conn.rollback();
                }
                throw e;
            }
        }
    }

    /**
     * 全てのメッセージを取得します
     *
     * @return メッセージのリスト
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, text, created_at FROM messages ORDER BY created_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message(
                        rs.getInt("id"),
                        rs.getString("text"),
                        rs.getLong("created_at")
                );
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * IDでメッセージを取得します
     *
     * @param id メッセージID
     * @return メッセージオブジェクト、見つからない場合はnull
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public Message getMessageById(int id) throws SQLException {
        String sql = "SELECT id, text, created_at FROM messages WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Message(
                            rs.getInt("id"),
                            rs.getString("text"),
                            rs.getLong("created_at")
                    );
                }
            }
        }

        return null;
    }

    /**
     * 最新のメッセージを取得します
     *
     * @return 最新のメッセージ、存在しない場合はnull
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public Message getLatestMessage() throws SQLException {
        String sql = "SELECT id, text, created_at FROM messages ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return new Message(
                        rs.getInt("id"),
                        rs.getString("text"),
                        rs.getLong("created_at")
                );
            }
        }

        return null;
    }

    /**
     * メッセージを更新します
     *
     * @param message 更新するメッセージオブジェクト
     * @return 更新された行数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int updateMessage(Message message) throws SQLException {
        String text = MessageValidator.normalize(message.getText());
        String sql = "UPDATE messages SET text = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, text);
            pstmt.setInt(2, message.getId());

            return pstmt.executeUpdate();
        }
    }

    /**
     * メッセージを削除します
     *
     * @param id 削除するメッセージのID
     * @return 削除された行数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int deleteMessage(int id) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                int deletedRows = pstmt.executeUpdate();

                // 全てのメッセージが削除された場合、デフォルトメッセージを追加
                if (deletedRows > 0 && getMessageCount(conn) == 0) {
                    insertDefaultMessage(conn);
                }

                conn.commit();
                return deletedRows;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

    }

    /**
     * 全てのメッセージを削除します
     *
     * @return 削除された行数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int deleteAllMessages() throws SQLException {
        String sql = "DELETE FROM messages";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int deletedRows = pstmt.executeUpdate();

                // 削除後にデフォルトメッセージを追加
                if (deletedRows > 0) {
                    insertDefaultMessage(conn);
                }

                conn.commit();
                return deletedRows;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 指定されたメッセージをまとめて削除します。
     *
     * @param ids 削除するメッセージID
     * @return 削除された行数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int deleteMessages(List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) {
            return 0;
        }
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM messages WHERE id = ?")) {
                for (int id : ids) {
                    pstmt.setInt(1, id);
                    pstmt.addBatch();
                }
                int deletedRows = 0;
                for (int result : pstmt.executeBatch()) {
                    if (result > 0) {
                        deletedRows += result;
                    }
                }
                if (deletedRows > 0 && getMessageCount(conn) == 0) {
                    insertDefaultMessage(conn);
                }
                conn.commit();
                return deletedRows;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * デフォルトメッセージを挿入します（削除後の復旧用）
     *
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private void insertDefaultMessage(Connection conn) throws SQLException {
        Message defaultMessage = new Message(DEFAULT_MESSAGE, System.currentTimeMillis());
        String sql = "INSERT INTO messages (text, created_at) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, defaultMessage.getText());
            pstmt.setLong(2, defaultMessage.getCreatedAt());
            pstmt.executeUpdate();
        }

        LOGGER.info(() -> "デフォルトメッセージを復旧しました: " + DEFAULT_MESSAGE);
    }

    /**
     * メッセージ数を取得します
     *
     * @return メッセージ数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    public int getMessageCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return getMessageCount(rs);
        }
    }

    /**
     * 指定された接続でメッセージ数を取得します。
     *
     * @param conn 使用するデータベース接続
     * @return メッセージ数
     * @throws SQLException データベース操作エラーが発生した場合
     */
    private int getMessageCount(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return getMessageCount(rs);
        }
    }

    /**
     * クエリ結果からメッセージ数を取得します。
     *
     * @param rs COUNTクエリの結果
     * @return メッセージ数
     * @throws SQLException 結果の読み取りに失敗した場合
     */
    private int getMessageCount(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }
}
