package com.example.gitguiwindowsapp.model;

/**
 * メッセージエンティティクラス
 *
 * <p>アプリケーションで表示するメッセージのデータモデルです。
 * SQLiteデータベースのmessagesテーブルに対応しています。</p>
 *
 * @author git-gui-windows-app
 * @version 0.2.0
 * @since 0.2.0
 */
public class Message {

    /**
     * メッセージID（プライマリキー）
     */
    private int id;

    /**
     * メッセージテキスト
     */
    private String text;

    /**
     * メッセージが作成された日時（UNIXタイムスタンプ）
     */
    private long createdAt;

    /**
     * デフォルトコンストラクタ
     */
    public Message() {
    }

    /**
     * 全フィールドを指定するコンストラクタ
     *
     * @param id        メッセージID
     * @param text      メッセージテキスト
     * @param createdAt 作成日時（UNIXタイムスタンプ）
     */
    public Message(int id, String text, long createdAt) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
    }

    /**
     * IDなしのコンストラクタ（新規作成時用）
     *
     * @param text      メッセージテキスト
     * @param createdAt 作成日時（UNIXタイムスタンプ）
     */
    public Message(String text, long createdAt) {
        this.text = text;
        this.createdAt = createdAt;
    }

    // Getter / Setter メソッド

    /**
     * メッセージIDを取得します
     *
     * @return メッセージID
     */
    public int getId() {
        return id;
    }

    /**
     * メッセージIDを設定します
     *
     * @param id メッセージID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * メッセージテキストを取得します
     *
     * @return メッセージテキスト
     */
    public String getText() {
        return text;
    }

    /**
     * メッセージテキストを設定します
     *
     * @param text メッセージテキスト
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * 作成日時を取得します
     *
     * @return 作成日時（UNIXタイムスタンプ）
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * 作成日時を設定します
     *
     * @param createdAt 作成日時（UNIXタイムスタンプ）
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * オブジェクトの文字列表現を返します
     *
     * @return 文字列表現
     */
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
