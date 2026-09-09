package com.example.gitguiwindowsapp.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;

import java.util.Objects;

/**
 * JavaFXコントロールへのテーマ適用だけを担当します。
 */
public final class ThemeService {
    private static final String STYLESHEET = "/styles.css";

    public void apply(Node root, Button themeButton, boolean darkMode) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(themeButton, "themeButton");
        root.getStyleClass().remove("dark");
        if (darkMode) {
            root.getStyleClass().add("dark");
        }
        themeButton.setText(darkMode ? "☾" : "☀");
        themeButton.setAccessibleText(darkMode ? "ライトモードに切り替え" : "ダークモードに切り替え");
    }

    public void apply(Node root, RepositoryToolbar toolbar, boolean darkMode) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(toolbar, "toolbar");
        root.getStyleClass().remove("dark");
        if (darkMode) {
            root.getStyleClass().add("dark");
        }
        toolbar.applyTheme(darkMode);
    }

    public void styleDialog(Dialog<?> dialog, boolean darkMode) {
        Objects.requireNonNull(dialog, "dialog");
        if (!darkMode) {
            return;
        }
        String stylesheet = ThemeService.class.getResource(STYLESHEET).toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
        dialog.getDialogPane().getStyleClass().add("dark");
    }
}
