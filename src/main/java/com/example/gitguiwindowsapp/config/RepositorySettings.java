package com.example.gitguiwindowsapp.config;

import java.nio.file.Path;
import java.util.List;

/**
 * 開く基準フォルダーと最近のリポジトリ設定のカテゴリです。
 */
public record RepositorySettings(Path openBaseDirectory, List<String> recentRepositories) {
    public RepositorySettings {
        openBaseDirectory = openBaseDirectory == null
                ? null : openBaseDirectory.toAbsolutePath().normalize();
        recentRepositories = List.copyOf(recentRepositories == null
                ? List.of() : recentRepositories);
    }
}
