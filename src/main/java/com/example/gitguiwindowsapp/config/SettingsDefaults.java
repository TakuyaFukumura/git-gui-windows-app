package com.example.gitguiwindowsapp.config;

/**
 * 設定値の既定値を一箇所で管理します。
 */
final class SettingsDefaults {
    static final boolean DARK_MODE = false;
    static final double WINDOW_WIDTH = 800;
    static final double WINDOW_HEIGHT = 600;
    static final double WINDOW_POSITION = Double.NaN;
    static final String GIT_EXECUTABLE = "git";
    static final int MAX_RECENT_REPOSITORIES = 5;

    private SettingsDefaults() {
    }
}
