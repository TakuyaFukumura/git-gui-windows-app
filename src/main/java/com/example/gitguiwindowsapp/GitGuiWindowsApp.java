package com.example.gitguiwindowsapp;

import com.example.gitguiwindowsapp.config.ApplicationSettings;
import com.example.gitguiwindowsapp.application.AppState;
import com.example.gitguiwindowsapp.application.OperationCoordinator;
import com.example.gitguiwindowsapp.git.GitCommandException;
import com.example.gitguiwindowsapp.git.GitCommandRunner;
import com.example.gitguiwindowsapp.git.GitService;
import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.CommitReference;
import com.example.gitguiwindowsapp.model.CommitReferenceType;
import com.example.gitguiwindowsapp.model.DiffDocument;
import com.example.gitguiwindowsapp.model.DiffLine;
import com.example.gitguiwindowsapp.model.DiffLineType;
import com.example.gitguiwindowsapp.model.FileChange;
import com.example.gitguiwindowsapp.model.GitOperationResult;
import com.example.gitguiwindowsapp.model.RepositoryInfo;
import com.example.gitguiwindowsapp.ui.CommitGraphView;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GitGuiWindowsApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(GitGuiWindowsApp.class.getName());
    private static final String APP_VERSION = "0.7.0";

    private ApplicationSettings settings;
    private GitService gitService;
    private final AppState appState = new AppState();
    private final OperationCoordinator operationCoordinator = new OperationCoordinator();
    private Stage stage;
    private BorderPane root;
    private final TableView<FileChange> changesTable = new TableView<>();
    private final ListView<DiffLine> diffView = new ListView<>();
    private final ListView<CommitEntry> historyView = new ListView<>();
    private final TextField historySearchField = new TextField();
    private final Label commitDetailId = new Label();
    private final Label commitDetailAuthor = new Label();
    private final Label commitDetailDate = new Label();
    private final Label commitDetailParents = new Label();
    private final Label commitDetailReferences = new Label();
    private final TextArea commitDetailMessage = new TextArea();
    private final TextField repositoryField = new TextField();
    private final TextField commitMessage = new TextField();
    private final ComboBox<BranchInfo> branchBox = new ComboBox<>();
    private final Label statusLabel = new Label("リポジトリを選択してください");
    private final Label repositoryLabel = new Label("未選択");
    private final Button refreshButton = new Button("更新");
    private final Button historyRefreshButton = new Button("履歴を更新");
    private final Button stageButton = new Button("ステージ");
    private final Button unstageButton = new Button("アンステージ");
    private final Button deleteBranchButton = new Button("削除");
    private final Button themeButton = new Button();

    public static void main(String[] args) {
        launch(args);
    }

    private HBox createHistoryToolbar() {
        Label searchLabel = new Label("履歴検索");
        historySearchField.setPromptText("メッセージ、作者、参照を検索");
        historySearchField.setTooltip(new Tooltip("コミットメッセージ、作者、参照名を検索"));
        historySearchField.textProperty().addListener((observable, oldValue, newValue) ->
                applyHistoryFilter(newValue));
        HBox.setHgrow(historySearchField, Priority.ALWAYS);
        HBox toolbar = new HBox(8, searchLabel, historySearchField);
        toolbar.getStyleClass().add("history-toolbar");
        return toolbar;
    }

    private void applyHistoryFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        appState.setHistoryQuery(normalized);
        historyView.setItems(FXCollections.observableArrayList(appState.filteredHistory()));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        settings = ApplicationSettings.load();
        gitService = new GitService(new GitCommandRunner(settings.getGitExecutable()));

        root = createLayout();
        Scene scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        String stylesheet = GitGuiWindowsApp.class.getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
        applyTheme(settings.isDarkMode());
        primaryStage.setTitle("Git GUI Windows App");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(480);
        if (Double.isFinite(settings.getWindowX())) {
            primaryStage.setX(settings.getWindowX());
        }
        if (Double.isFinite(settings.getWindowY())) {
            primaryStage.setY(settings.getWindowY());
        }
        primaryStage.setOnCloseRequest(event -> saveSettings());
        primaryStage.show();

        if (!settings.getRecentRepositories().isEmpty()) {
            openRepository(Path.of(settings.getRecentRepositories().get(0)));
        }
    }

    private BorderPane createLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(createRepositoryBar());
        root.setCenter(createChangeView());
        root.setBottom(createActionBar());
        configureTable();
        return root;
    }

    private HBox createRepositoryBar() {
        repositoryField.setPromptText("Gitリポジトリのパス");
        repositoryField.setTooltip(new Tooltip("リポジトリのフォルダーを入力"));
        HBox.setHgrow(repositoryField, Priority.ALWAYS);
        Button browse = new Button("開く...");
        browse.setOnAction(event -> chooseRepository());
        Button setOpenBaseDirectory = new Button("基準フォルダー...");
        setOpenBaseDirectory.setTooltip(new Tooltip("「開く...」の初期フォルダーを設定"));
        setOpenBaseDirectory.setOnAction(event -> chooseOpenBaseDirectory());
        refreshButton.setDisable(true);
        refreshButton.setOnAction(event -> refreshRepository());

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
        branchBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateBranchButtons(newValue));
        Button switchBranch = new Button("切替");
        switchBranch.setOnAction(event -> switchBranch());
        Button newBranch = new Button("新規ブランチ");
        newBranch.setOnAction(event -> createBranch());
        deleteBranchButton.setOnAction(event -> deleteBranch());
        deleteBranchButton.setDisable(true);
        themeButton.getStyleClass().add("theme-toggle");
        themeButton.setOnAction(event -> toggleTheme());
        themeButton.setTooltip(new Tooltip("ライトモードとダークモードを切り替え"));
        repositoryLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(repositoryLabel, Priority.ALWAYS);
        HBox bar = new HBox(8, repositoryField, browse, setOpenBaseDirectory, refreshButton, repositoryLabel,
                branchBox, switchBranch, newBranch, deleteBranchButton, themeButton);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private SplitPane createChangeView() {
        changesTable.setPlaceholder(new Label("変更ファイルはありません"));
        changesTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showDiff(newValue));
        diffView.getStyleClass().add("diff-view");
        diffView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(DiffLine line, boolean empty) {
                super.updateItem(line, empty);
                getStyleClass().removeAll("diff-addition", "diff-deletion", "diff-header",
                        "diff-hunk", "diff-meta", "diff-context");
                if (empty || line == null) {
                    setText(null);
                    return;
                }
                setText(line.text());
                getStyleClass().add(styleClassFor(line.type()));
            }
        });
        SplitPane split = new SplitPane(changesTable, diffView);
        split.setDividerPositions(0.38);
        Tab changesTab = new Tab("変更", split);
        changesTab.setClosable(false);

        historyView.getStyleClass().add("history-view");
        historyView.setPlaceholder(new Label("コミット履歴はありません"));
        historyView.setFixedCellSize(42);
        historyView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showCommitDetails(newValue));
        historyView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CommitEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(createCommitRow(entry));
            }
        });
        historyRefreshButton.setDisable(true);
        historyRefreshButton.setOnAction(event -> refreshHistory());
        SplitPane historySplit = new SplitPane(historyView, createCommitDetails());
        historySplit.setDividerPositions(0.7);
        VBox historyContent = new VBox(8, createHistoryToolbar(), historySplit);
        VBox.setVgrow(historySplit, Priority.ALWAYS);
        Tab historyTab = new Tab("履歴", historyContent);
        historyTab.setClosable(false);
        TabPane tabs = new TabPane(changesTab, historyTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return new SplitPane(tabs);
    }

    private void configureTable() {
        TableColumn<FileChange, String> path = new TableColumn<>("ファイル");
        path.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().path()));
        path.setPrefWidth(420);
        TableColumn<FileChange, String> type = new TableColumn<>("変更");
        type.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().type().name()));
        TableColumn<FileChange, String> stage = new TableColumn<>("状態");
        stage.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().stageState().name()));
        changesTable.getColumns().addAll(path, type, stage);
    }

    private HBox createActionBar() {
        stageButton.setDisable(true);
        unstageButton.setDisable(true);
        stageButton.setOnAction(event -> stageSelected());
        unstageButton.setOnAction(event -> unstageSelected());
        commitMessage.setPromptText("コミットメッセージ");
        HBox.setHgrow(commitMessage, Priority.ALWAYS);
        Button commit = new Button("コミット");
        commit.setOnAction(event -> commit());
        HBox bar = new HBox(8, stageButton, unstageButton, commitMessage, commit,
                historyRefreshButton, statusLabel);
        bar.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(statusLabel, Priority.NEVER);
        return bar;
    }

    private void chooseRepository() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Gitリポジトリを選択");
        Path baseDirectory = settings.getOpenBaseDirectory();
        if (baseDirectory != null && Files.isDirectory(baseDirectory)) {
            chooser.setInitialDirectory(baseDirectory.toFile());
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            openRepository(selected.toPath());
        }
    }

    private void chooseOpenBaseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("「開く...」の基準フォルダーを選択");
        Path baseDirectory = settings.getOpenBaseDirectory();
        if (baseDirectory != null && Files.isDirectory(baseDirectory)) {
            chooser.setInitialDirectory(baseDirectory.toFile());
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            settings.setOpenBaseDirectory(selected.toPath());
            saveSettings();
            statusLabel.setText("開く...の基準フォルダーを設定しました");
        }
    }

    private void openRepository(Path path) {
        if (path == null) {
            return;
        }
        setBusy(true, "リポジトリを確認中...");
        operationCoordinator.execute(() -> {
            RepositoryInfo info = gitService.status(path);
            List<BranchInfo> branches = gitService.branches(info.root());
            return new RepositorySnapshot(info, branches);
        }, snapshot -> {
            appState.setRepository(snapshot.info(), snapshot.branches());
            repositoryField.setText(appState.repository().toString());
            settings.addRecentRepository(appState.repository());
            updateView(snapshot);
            refreshHistory();
            setBusy(false, "準備完了");
            saveSettings();
        }, this::handleOperationFailure, "リポジトリを開く");
    }

    private void refreshRepository() {
        if (appState.repository() != null) {
            openRepository(appState.repository());
        }
    }

    private void updateView(RepositorySnapshot snapshot) {
        repositoryLabel.setText(snapshot.info().root() + "  [" + snapshot.info().branch() + "]"
                + (snapshot.info().clean() ? "  clean" : "  変更あり"));
        changesTable.setItems(FXCollections.observableArrayList(snapshot.info().changes()));
        branchBox.setItems(FXCollections.observableArrayList(snapshot.branches()));
        snapshot.branches().stream().filter(BranchInfo::current).findFirst().ifPresent(branchBox::setValue);
        updateBranchButtons(branchBox.getValue());
        updateSelectionButtons(changesTable.getSelectionModel().getSelectedItem());
    }

    private void refreshHistory() {
        if (appState.repository() == null) {
            appState.setHistory(List.of());
            historyView.getItems().clear();
            return;
        }
        historyRefreshButton.setDisable(true);
        String selectedId = Optional.ofNullable(historyView.getSelectionModel().getSelectedItem())
                .map(CommitEntry::id).orElse(null);
        operationCoordinator.execute(() -> gitService.commitHistory(appState.repository()), entries -> {
            appState.setHistory(entries);
            applyHistoryFilter(historySearchField.getText());
            if (selectedId != null) {
                historyView.getItems().stream().filter(entry -> entry.id().equals(selectedId)).findFirst()
                        .ifPresent(entry -> historyView.getSelectionModel().select(entry));
            }
            historyRefreshButton.setDisable(false);
        }, this::handleOperationFailure, "コミット履歴の取得");
    }

    private HBox createCommitRow(CommitEntry entry) {
        CommitGraphView graph = new CommitGraphView(entry);
        HBox.setHgrow(graph, Priority.NEVER);

        HBox references = new HBox(4);
        references.getStyleClass().add("commit-references");
        for (CommitReference reference : entry.references()) {
            Label badge = new Label(reference.name());
            badge.getStyleClass().add("commit-reference");
            badge.getStyleClass().add(referenceClass(reference.type()));
            badge.setTooltip(new Tooltip(reference.name()));
            references.getChildren().add(badge);
        }

        references.setMaxWidth(260);

        Label subject = new Label(entry.subject());
        subject.getStyleClass().add("commit-subject");
        subject.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        subject.setTooltip(new Tooltip(entry.subject()));
        HBox.setHgrow(subject, Priority.ALWAYS);

        Label metadata = new Label(entry.shortId() + "  " + entry.authorName() + "  "
                + DateTimeFormatter.ISO_LOCAL_DATE.format(entry.committedAt()));
        metadata.getStyleClass().add("commit-metadata");
        return new HBox(8, graph, references, subject, metadata);
    }

    private VBox createCommitDetails() {
        Label title = new Label("コミット詳細");
        title.getStyleClass().add("commit-detail-title");
        commitDetailMessage.setEditable(false);
        commitDetailMessage.setWrapText(true);
        commitDetailMessage.setPrefRowCount(8);
        commitDetailMessage.setPromptText("コミットを選択してください");
        commitDetailMessage.getStyleClass().add("commit-detail-message");
        commitDetailId.getStyleClass().add("commit-detail-value");
        commitDetailAuthor.getStyleClass().add("commit-detail-value");
        commitDetailDate.getStyleClass().add("commit-detail-value");
        commitDetailParents.getStyleClass().add("commit-detail-value");
        commitDetailReferences.getStyleClass().add("commit-detail-value");
        VBox details = new VBox(8, title,
                detailLine("ID", commitDetailId),
                detailLine("作者", commitDetailAuthor),
                detailLine("日時", commitDetailDate),
                detailLine("親", commitDetailParents),
                detailLine("参照", commitDetailReferences),
                commitDetailMessage);
        details.getStyleClass().add("commit-details");
        details.setPadding(new Insets(10));
        showCommitDetails(null);
        return details;
    }

    private static VBox detailLine(String name, Label value) {
        Label label = new Label(name);
        label.getStyleClass().add("commit-detail-label");
        return new VBox(2, label, value);
    }

    private void showCommitDetails(CommitEntry entry) {
        boolean empty = entry == null;
        commitDetailId.setText(empty ? "" : entry.id());
        commitDetailAuthor.setText(empty ? "" : entry.authorName() + " <" + entry.authorEmail() + ">");
        commitDetailDate.setText(empty ? "" : entry.committedAt().toString());
        commitDetailParents.setText(empty ? "" : entry.parents().isEmpty()
                ? "なし" : String.join(", ", entry.parents()));
        commitDetailReferences.setText(empty ? "" : entry.references().isEmpty()
                ? "なし" : entry.references().stream().map(CommitReference::name).toList().toString());
        commitDetailMessage.setText(empty ? "" : entry.message());
    }

    private static String referenceClass(CommitReferenceType type) {
        return switch (type) {
            case HEAD -> "commit-reference-head";
            case LOCAL_BRANCH -> "commit-reference-branch";
            case REMOTE_BRANCH -> "commit-reference-remote";
            case TAG -> "commit-reference-tag";
        };
    }

    private void showDiff(FileChange change) {
        updateSelectionButtons(change);
        if (change == null || appState.repository() == null) {
            diffView.getItems().clear();
            return;
        }
        boolean cached = change.isStaged() && !change.isUnstaged();
        setBusy(true, "差分を読み込み中...");
        operationCoordinator.execute(() -> gitService.diff(appState.repository(), change.path(), cached), diff -> {
            diffView.setItems(FXCollections.observableArrayList(formatDiff(diff)));
            setBusy(false, "準備完了");
        }, this::handleOperationFailure, "差分の取得");
    }

    private List<DiffLine> formatDiff(DiffDocument diff) {
        if (diff.tooLarge()) {
            return List.of(new DiffLine(DiffLineType.META,
                    "差分が10 MBを超えるため表示できません。"));
        }
        if (diff.binary()) {
            List<DiffLine> lines = new ArrayList<>();
            lines.add(new DiffLine(DiffLineType.META, "バイナリファイルのため内容を表示できません。"));
            lines.add(new DiffLine(DiffLineType.META, ""));
            lines.addAll(linesFor(diff.rawText()));
            return lines;
        }
        if (!diff.rawText().isBlank()) {
            List<DiffLine> lines = diff.files().stream()
                    .flatMap(file -> file.lines().stream())
                    .toList();
            return lines.isEmpty() ? linesFor(diff.rawText()) : lines;
        }
        return List.of(new DiffLine(DiffLineType.META, "差分はありません。"));
    }

    private static List<DiffLine> linesFor(String text) {
        List<DiffLine> lines = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            lines.add(new DiffLine(DiffLineType.META, line));
        }
        return lines;
    }

    private static String styleClassFor(DiffLineType type) {
        return switch (type) {
            case ADDITION -> "diff-addition";
            case DELETION -> "diff-deletion";
            case HEADER -> "diff-header";
            case HUNK -> "diff-hunk";
            case META -> "diff-meta";
            case CONTEXT -> "diff-context";
        };
    }

    private void updateSelectionButtons(FileChange change) {
        stageButton.setDisable(!appState.canStage(change));
        unstageButton.setDisable(!appState.canUnstage(change));
    }

    private void stageSelected() {
        FileChange selected = changesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            runOperation(() -> gitService.stage(appState.repository(), selected.path()), "ステージ");
        }
    }

    private void unstageSelected() {
        FileChange selected = changesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            runOperation(() -> gitService.unstage(appState.repository(), selected.path()), "アンステージ");
        }
    }

    private void commit() {
        if (appState.repository() == null || commitMessage.getText().isBlank()) {
            showError("コミット", "コミットメッセージを入力してください。");
            return;
        }
        runOperation(() -> gitService.commit(appState.repository(), commitMessage.getText()),
                "コミット");
        commitMessage.clear();
    }

    private void createBranch() {
        if (appState.repository() == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新規ブランチ");
        dialog.setHeaderText("作成するブランチ名");
        styleDialog(dialog);
        Optional<String> name = dialog.showAndWait();
        if (name.isPresent() && !name.get().isBlank()) {
            runOperation(() -> gitService.createBranch(appState.repository(), name.get()), "ブランチ作成");
        }
    }

    private void switchBranch() {
        BranchInfo selected = branchBox.getValue();
        if (appState.repository() != null && selected != null && !selected.current()) {
            if (appState.repositoryInfo() != null && !appState.repositoryInfo().clean()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("ブランチ切替");
                alert.setHeaderText("未コミットの変更があります。");
                alert.setContentText("変更を保持したままブランチを切り替えますか？");
                alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
                styleDialog(alert);
                if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }
            runOperation(() -> gitService.checkout(appState.repository(), selected.name()), "ブランチ切替");
        }
    }

    private void deleteBranch() {
        BranchInfo selected = branchBox.getValue();
        if (!appState.canDeleteBranch(selected)) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("ブランチ削除");
        alert.setHeaderText("ブランチ「" + selected.name() + "」を削除しますか？");
        alert.setContentText("マージ済みブランチのみ安全に削除できます。");
        styleDialog(alert);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            runOperation(() -> gitService.deleteBranch(appState.repository(), selected.name()), "ブランチ削除");
        }
    }

    private void runOperation(Callable<GitOperationResult> operation, String label) {
        setBusy(true, label + "中...");
        operationCoordinator.execute(operation, ignored -> {
            setBusy(false, "完了");
            refreshRepository();
        }, this::handleOperationFailure, label);
    }

    private void setBusy(boolean busy, String message) {
        appState.setOperationRunning(busy);
        refreshButton.setDisable(busy || appState.repository() == null);
        historyRefreshButton.setDisable(busy || appState.repository() == null);
        statusLabel.setText(message);
        updateSelectionButtons(changesTable.getSelectionModel().getSelectedItem());
        updateBranchButtons(branchBox.getValue());
    }

    private void updateBranchButtons(BranchInfo branch) {
        deleteBranchButton.setDisable(!appState.canDeleteBranch(branch));
    }

    private void handleOperationFailure(Throwable error) {
        setBusy(false, "エラー");
        if (error instanceof GitCommandException gitError) {
            showError("Git操作", gitError.getMessage());
        } else {
            showError("操作", error == null ? "不明なエラー" : error.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? "操作に失敗しました。" : message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {
        if (!settings.isDarkMode()) {
            return;
        }
        String stylesheet = GitGuiWindowsApp.class.getResource("/styles.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
        dialog.getDialogPane().getStyleClass().add("dark");
    }

    private void toggleTheme() {
        boolean darkMode = !settings.isDarkMode();
        settings.setDarkMode(darkMode);
        applyTheme(darkMode);
        saveSettings();
    }

    private void applyTheme(boolean darkMode) {
        if (root == null) {
            return;
        }
        root.getStyleClass().remove("dark");
        if (darkMode) {
            root.getStyleClass().add("dark");
        }
        themeButton.setText(darkMode ? "☾" : "☀");
        themeButton.setAccessibleText(darkMode ? "ライトモードに切り替え" : "ダークモードに切り替え");
    }

    private void saveSettings() {
        if (stage == null || settings == null) {
            return;
        }
        settings.setWindowWidth(stage.getWidth());
        settings.setWindowHeight(stage.getHeight());
        settings.setWindowX(stage.getX());
        settings.setWindowY(stage.getY());
        try {
            settings.save();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "設定の保存に失敗しました。", e);
        }
    }

    private record RepositorySnapshot(RepositoryInfo info, List<BranchInfo> branches) {
    }
}
