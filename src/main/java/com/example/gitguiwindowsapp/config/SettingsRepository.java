package com.example.gitguiwindowsapp.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 設定ファイルの場所と永続化処理を担当します。
 */
public final class SettingsRepository {
    public ApplicationSettings load() throws IOException {
        return ApplicationSettings.load();
    }

    public ApplicationSettings load(Path path) throws IOException {
        return ApplicationSettings.load(path);
    }

    public void save(ApplicationSettings settings) throws IOException {
        if (settings == null) {
            throw new IllegalArgumentException("Settings must not be null.");
        }
        settings.save();
    }
}
