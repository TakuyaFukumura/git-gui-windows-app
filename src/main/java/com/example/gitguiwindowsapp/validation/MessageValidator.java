package com.example.gitguiwindowsapp.validation;

/**
 * メッセージ入力を検証・正規化するユーティリティです。
 */
public final class MessageValidator {

    public static final int MAX_LENGTH = 1_000;

    private MessageValidator() {
    }

    /**
     * 入力を前後の空白を除いた本文へ正規化します。
     *
     * @param text 入力本文
     * @return 正規化された本文
     * @throws IllegalArgumentException 本文がnullまたは空白だけの場合
     */
    public static String normalize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("メッセージはnullにできません。");
        }

        if (text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("メッセージに制御文字は使用できません。");
        }

        String normalized = text.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("メッセージを入力してください。");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "メッセージは" + MAX_LENGTH + "文字以内で入力してください。");
        }
        return normalized;
    }
}
