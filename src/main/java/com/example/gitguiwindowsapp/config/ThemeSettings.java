package com.example.gitguiwindowsapp.config;

/**
 * JavaFXテーマ選択の永続化値です。適用処理はUI層のThemeServiceが担当します。
 */
public record ThemeSettings(boolean darkMode) {
}
