package com.example.gitguiwindowsapp.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.example.gitguiwindowsapp.model.BranchInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * リポジトリ選択、更新、ブランチ操作、テーマ切替の操作部品です。
 */
public final class RepositoryToolbar {
    private final TextField repositoryField = new TextField();
    private final Label repositoryLabel = new Label("未選択");
    private final ComboBox<BranchInfo> branchBox = new ComboBox<>();
    private final Button refreshButton = new Button("更新");
    private final Button deleteBranchButton = new Button("削除");
    private final Button themeButton = new Button();
    private final HBox view;

    public RepositoryToolbar(Runnable chooseRepository, Runnable chooseBaseDirectory,
                             Runnable refreshRepository, Runnable switchBranch,
                             Runnable createBranch, Runnable deleteBranch,
                             Runnable toggleTheme, Consumer<BranchInfo> branchChanged) {
        repositoryField.setPromptText("Gitリポジトリのパス");
        repositoryField.setTooltip(new Tooltip("リポジトリのフォルダーを入力"));
        HBox.setHgrow(repositoryField, Priority.ALWAYS);

        Button browse = new Button("開く...");
        browse.setOnAction(event -> chooseRepository.run());
        Button setOpenBaseDirectory = new Button("基準フォルダー...");
        setOpenBaseDirectory.setTooltip(new Tooltip("「開く...」の初期フォルダーを設定"));
        setOpenBaseDirectory.setOnAction(event -> chooseBaseDirectory.run());
        refreshButton.setDisable(true);
        refreshButton.setOnAction(event -> refreshRepository.run());

        branchBox.setPromptText("ブランチ");
        branchBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BranchInfo branch) {
                return branch == null ? "" : branch.name();
            }

            @Override
            public BranchInfo fromString(String value) {
                return null;
            }
        });
        branchBox.valueProperty().addListener((observable, oldValue, newValue) -> branchChanged.accept(newValue));
        Button switchButton = new Button("切替");
        switchButton.setOnAction(event -> switchBranch.run());
        Button newBranch = new Button("新規ブランチ");
        newBranch.setOnAction(event -> createBranch.run());
        deleteBranchButton.setOnAction(event -> deleteBranch.run());
        deleteBranchButton.setDisable(true);
        themeButton.getStyleClass().add("theme-toggle");
        themeButton.setOnAction(event -> toggleTheme.run());
        themeButton.setTooltip(new Tooltip("ライトモードとダークモードを切り替え"));
        repositoryLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(repositoryLabel, Priority.ALWAYS);

        view = new HBox(8, repositoryField, browse, setOpenBaseDirectory, refreshButton,
                repositoryLabel, branchBox, switchButton, newBranch, deleteBranchButton, themeButton);
        view.setPadding(new Insets(0, 0, 10, 0));
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "The live JavaFX container must be returned for composition into the application scene.")
    public HBox view() {
        return view;
    }

    public void setRepository(Path repository) {
        repositoryField.setText(repository == null ? "" : repository.toString());
    }

    public void updateRepository(String text) {
        repositoryLabel.setText(text);
    }

    public void setBranches(List<BranchInfo> branches) {
        branchBox.getItems().setAll(branches);
        branches.stream().filter(BranchInfo::current).findFirst().ifPresent(branchBox::setValue);
    }

    public BranchInfo selectedBranch() {
        return branchBox.getValue();
    }

    public void setBusy(boolean busy, boolean hasRepository) {
        refreshButton.setDisable(busy || !hasRepository);
    }

    public void setDeleteBranchEnabled(boolean enabled) {
        deleteBranchButton.setDisable(!enabled);
    }

    public void applyTheme(boolean darkMode) {
        themeButton.setText(darkMode ? "☾" : "☀");
        themeButton.setAccessibleText(darkMode ? "ライトモードに切り替え" : "ダークモードに切り替え");
    }
}
