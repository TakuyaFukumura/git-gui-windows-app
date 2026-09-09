package com.example.gitguiwindowsapp.config;

import java.util.Objects;

/**
 * Git実行ファイル設定のカテゴリです。
 */
public record GitSettings(String executable) {
    public GitSettings {
        executable = Objects.requireNonNull(executable, "executable").trim();
        if (executable.isEmpty()) {
            throw new IllegalArgumentException("Git executable must not be blank.");
        }
    }
}
