package com.example.gitguiwindowsapp;

import com.example.gitguiwindowsapp.application.AppState;
import com.example.gitguiwindowsapp.config.ApplicationSettings;
import com.example.gitguiwindowsapp.config.SettingsRepository;
import com.example.gitguiwindowsapp.git.BranchService;
import com.example.gitguiwindowsapp.git.ChangeService;
import com.example.gitguiwindowsapp.git.CommitService;
import com.example.gitguiwindowsapp.git.GitCommandException;
import com.example.gitguiwindowsapp.git.GitCommandRunner;
import com.example.gitguiwindowsapp.git.HistoryService;
import com.example.gitguiwindowsapp.git.RepositoryService;
import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.DiffDocument;
import com.example.gitguiwindowsapp.model.DiffLine;
import com.example.gitguiwindowsapp.model.DiffLineType;
import com.example.gitguiwindowsapp.model.FileChange;
import com.example.gitguiwindowsapp.model.GitOperationResult;
import com.example.gitguiwindowsapp.model.RepositoryInfo;
import com.example.gitguiwindowsapp.ui.ChangesPane;
import com.example.gitguiwindowsapp.ui.CommitActionBar;
import com.example.gitguiwindowsapp.ui.DialogService;
import com.example.gitguiwindowsapp.ui.HistoryPane;
import com.example.gitguiwindowsapp.ui.OperationCoordinator;
import com.example.gitguiwindowsapp.ui.RepositoryToolbar;
import com.example.gitguiwindowsapp.ui.ThemeService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GitGuiWindowsApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(GitGuiWindowsApp.class.getName());

    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final AppState appState = new AppState();
    private final OperationCoordinator operationCoordinator = new OperationCoordinator();
    private final ThemeService themeService = new ThemeService();
    private final DialogService dialogService = new DialogService(themeService);
    private ApplicationSettings settings;
    private Stage stage;
    private BorderPane root;
    private RepositoryService repositoryService;
    private ChangeService changeService;
    private BranchService branchService;
    private HistoryService historyService;
    private CommitService commitService;
    private RepositoryToolbar repositoryToolbar;
    private ChangesPane changesPane;
    private CommitActionBar actionBar;
    private HistoryPane historyPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        settings = settingsRepository.load();
        GitCommandRunner runner = new GitCommandRunner(settings.getGitExecutable());
        repositoryService = new RepositoryService(runner);
        changeService = new ChangeService(runner);
        branchService = new BranchService(runner);
        historyService = new HistoryService(runner);
        commitService = new CommitService(runner);

        repositoryToolbar = new RepositoryToolbar(this::chooseRepository, this::chooseOpenBaseDirectory,
                this::refreshRepository, this::switchBranch, this::createBranch, this::deleteBranch,
                this::toggleTheme, this::updateBranchButtons);
        changesPane = new ChangesPane(this::showDiff);
        actionBar = new CommitActionBar(this::stageSelected, this::unstageSelected,
                this::commit, this::refreshHistory);
        historyPane = new HistoryPane(this::filterHistory, ignored -> {
        });
        root = createLayout();
        Scene scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        scene.getStylesheets().add(GitGuiWindowsApp.class.getResource("/styles.css").toExternalForm());
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
        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(repositoryToolbar.view());
        Tab changes = new Tab("変更", changesPane.view());
        changes.setClosable(false);
        Tab history = new Tab("履歴", historyPane.view());
        history.setClosable(false);
        TabPane tabs = new TabPane(changes, history);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(new SplitPane(tabs));
        root.setBottom(actionBar.view());
        return root;
    }

    private void chooseRepository() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Gitリポジトリを選択");
        Path base = settings.getOpenBaseDirectory();
        if (base != null && Files.isDirectory(base)) {
            chooser.setInitialDirectory(base.toFile());
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            openRepository(selected.toPath());
        }
    }

    private void chooseOpenBaseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("「開く...」の基準フォルダーを選択");
        Path base = settings.getOpenBaseDirectory();
        if (base != null && Files.isDirectory(base)) {
            chooser.setInitialDirectory(base.toFile());
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            settings.setOpenBaseDirectory(selected.toPath());
            saveSettings();
            actionBar.setStatus("開く...の基準フォルダーを設定しました");
        }
    }

    private void openRepository(Path path) {
        setBusy(true, "リポジトリを確認中...");
        operationCoordinator.execute(() -> {
            RepositoryInfo info = repositoryService.status(path);
            return new RepositorySnapshot(info, branchService.list(info.root()));
        }, snapshot -> {
            appState.setRepository(snapshot.info(), snapshot.branches());
            repositoryToolbar.setRepository(appState.repository());
            repositoryToolbar.updateRepository(snapshot.info().root() + "  [" + snapshot.info().branch() + "]"
                    + (snapshot.info().clean() ? "  clean" : "  変更あり"));
            repositoryToolbar.setBranches(snapshot.branches());
            changesPane.setChanges(snapshot.info().changes());
            settings.addRecentRepository(appState.repository());
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

    private void showDiff(FileChange change) {
        updateSelectionButtons(change);
        if (change == null || appState.repository() == null) {
            changesPane.clearDiff();
            return;
        }
        boolean cached = change.isStaged() && !change.isUnstaged();
        setBusy(true, "差分を読み込み中...");
        operationCoordinator.execute(() -> changeService.diff(appState.repository(), change.path(), cached),
                diff -> {
                    changesPane.setDiff(formatDiff(diff));
                    setBusy(false, "準備完了");
                }, this::handleOperationFailure, "差分の取得");
    }

    private void stageSelected() {
        FileChange selected = changesPane.selectedChange();
        if (selected != null) {
            runOperation(() -> changeService.stage(appState.repository(), selected.path()), "ステージ");
        }
    }

    private void unstageSelected() {
        FileChange selected = changesPane.selectedChange();
        if (selected != null) {
            runOperation(() -> changeService.unstage(appState.repository(), selected.path()), "アンステージ");
        }
    }

    private void commit() {
        if (appState.repository() == null || actionBar.commitMessage().isBlank()) {
            dialogService.showError("コミット", "コミットメッセージを入力してください。", settings.isDarkMode());
            return;
        }
        runOperation(() -> commitService.commit(appState.repository(), actionBar.commitMessage()), "コミット");
        actionBar.clearCommitMessage();
    }

    private void createBranch() {
        if (appState.repository() == null) {
            return;
        }
        Optional<String> name = dialogService.requestBranchName(settings.isDarkMode());
        if (name.isPresent() && !name.get().isBlank()) {
            runOperation(() -> branchService.create(appState.repository(), name.get()), "ブランチ作成");
        }
    }

    private void switchBranch() {
        BranchInfo selected = repositoryToolbar.selectedBranch();
        if (appState.repository() == null || selected == null || selected.current()) {
            return;
        }
        if (appState.repositoryInfo() != null && !appState.repositoryInfo().clean()
                && !dialogService.confirm("ブランチ切替", "未コミットの変更があります。",
                "変更を保持したままブランチを切り替えますか？", settings.isDarkMode())) {
            return;
        }
        runOperation(() -> branchService.checkout(appState.repository(), selected.name()), "ブランチ切替");
    }

    private void deleteBranch() {
        BranchInfo selected = repositoryToolbar.selectedBranch();
        if (!appState.canDeleteBranch(selected)) {
            return;
        }
        if (dialogService.confirm("ブランチ削除", "ブランチ「" + selected.name() + "」を削除しますか？",
                "マージ済みブランチのみ安全に削除できます。", settings.isDarkMode())) {
            runOperation(() -> branchService.delete(appState.repository(), selected.name()), "ブランチ削除");
        }
    }

    private void refreshHistory() {
        if (appState.repository() == null) {
            historyPane.setEntries(List.of());
            return;
        }
        operationCoordinator.execute(() -> historyService.list(appState.repository()), entries -> {
            appState.setHistory(entries);
            historyPane.setEntries(entries);
        }, this::handleOperationFailure, "コミット履歴の取得");
    }

    private void filterHistory(String query) {
        appState.setHistoryQuery(query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT));
        historyPane.applyFilter(query);
    }

    private void runOperation(Callable<GitOperationResult> operation, String label) {
        setBusy(true, label + "中...");
        operationCoordinator.execute(operation, ignored -> {
            setBusy(false, "完了");
            refreshRepository();
        }, this::handleOperationFailure, label);
    }

    private void setBusy(boolean busy, String message) {
        boolean hasRepository = appState.repository() != null;
        actionBar.setBusy(busy, hasRepository);
        repositoryToolbar.setBusy(busy, hasRepository);
        actionBar.setStatus(message);
        updateSelectionButtons(changesPane.selectedChange());
        updateBranchButtons(repositoryToolbar.selectedBranch());
    }

    private void updateSelectionButtons(FileChange change) {
        actionBar.setSelectionState(appState.canStage(change), appState.canUnstage(change));
    }

    private void updateBranchButtons(BranchInfo branch) {
        repositoryToolbar.setDeleteBranchEnabled(appState.canDeleteBranch(branch));
    }

    private void handleOperationFailure(Throwable error) {
        setBusy(false, "エラー");
        String message = error instanceof GitCommandException ? error.getMessage()
                : error == null ? "不明なエラー" : error.getMessage();
        dialogService.showError("操作", message, settings.isDarkMode());
    }

    private void toggleTheme() {
        settings.setDarkMode(!settings.isDarkMode());
        applyTheme(settings.isDarkMode());
        saveSettings();
    }

    private void applyTheme(boolean darkMode) {
        if (root != null) {
            themeService.apply(root, repositoryToolbar, darkMode);
        }
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
            settingsRepository.save(settings);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "設定の保存に失敗しました。", e);
        }
    }

    private static List<DiffLine> formatDiff(DiffDocument diff) {
        if (diff.tooLarge()) {
            return List.of(new DiffLine(DiffLineType.META, "差分が10 MBを超えるため表示できません。"));
        }
        if (diff.binary()) {
            List<DiffLine> lines = new ArrayList<>();
            lines.add(new DiffLine(DiffLineType.META, "バイナリファイルのため内容を表示できません。"));
            lines.add(new DiffLine(DiffLineType.META, ""));
            lines.addAll(linesFor(diff.rawText()));
            return lines;
        }
        if (!diff.rawText().isBlank()) {
            List<DiffLine> lines = diff.files().stream().flatMap(file -> file.lines().stream()).toList();
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

    private record RepositorySnapshot(RepositoryInfo info, List<BranchInfo> branches) {
    }
}
