package com.example.gitguiwindowsapp.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * 共通ダイアログの生成とテーマ適用を担当します。
 */
public final class DialogService {
    private final ThemeService themeService;

    public DialogService(ThemeService themeService) {
        this.themeService = themeService;
    }

    public Optional<String> requestBranchName(boolean darkMode) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新規ブランチ");
        dialog.setHeaderText("作成するブランチ名");
        themeService.styleDialog(dialog, darkMode);
        return dialog.showAndWait();
    }

    public boolean confirm(String title, String header, String content, boolean darkMode) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        themeService.styleDialog(alert, darkMode);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public void showError(String title, String message, boolean darkMode) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? "操作に失敗しました。" : message);
        themeService.styleDialog(alert, darkMode);
        alert.showAndWait();
    }
}
