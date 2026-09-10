package com.example.gitguiwindowsapp.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * ステージ、アンステージ、コミット、履歴更新の操作部品です。
 */
public final class CommitActionBar {
    private final Button stageButton = new Button("ステージ");
    private final Button unstageButton = new Button("アンステージ");
    private final Button historyRefreshButton = new Button("履歴を更新");
    private final TextField commitMessage = new TextField();
    private final Label statusLabel = new Label("リポジトリを選択してください");
    private final HBox view;

    public CommitActionBar(Runnable stage, Runnable unstage, Runnable commit, Runnable refreshHistory) {
        stageButton.setDisable(true);
        unstageButton.setDisable(true);
        stageButton.setOnAction(event -> stage.run());
        unstageButton.setOnAction(event -> unstage.run());
        commitMessage.setPromptText("コミットメッセージ");
        HBox.setHgrow(commitMessage, Priority.ALWAYS);
        Button commitButton = new Button("コミット");
        commitButton.setOnAction(event -> commit.run());
        historyRefreshButton.setDisable(true);
        historyRefreshButton.setOnAction(event -> refreshHistory.run());
        view = new HBox(8, stageButton, unstageButton, commitMessage, commitButton,
                historyRefreshButton, statusLabel);
        view.setPadding(new Insets(10, 0, 0, 0));
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "The live JavaFX container must be returned for composition into the application scene.")
    public HBox view() {
        return view;
    }

    public String commitMessage() {
        return commitMessage.getText();
    }

    public void clearCommitMessage() {
        commitMessage.clear();
    }

    public void setSelectionState(boolean canStage, boolean canUnstage) {
        stageButton.setDisable(!canStage);
        unstageButton.setDisable(!canUnstage);
    }

    public void setBusy(boolean busy, boolean hasRepository) {
        stageButton.setDisable(busy || !hasRepository);
        unstageButton.setDisable(busy || !hasRepository);
        historyRefreshButton.setDisable(busy || !hasRepository);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
