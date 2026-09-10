package com.example.gitguiwindowsapp.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * アプリケーション設定の保存と復元を担当します。
 *
 * <p>設定はユーザーのホームディレクトリ配下にProperties形式で保存します。
 * 未知のキーは無視し、壊れた値は既定値に戻すことで将来の互換性を保ちます。</p>
 *
 * <p>設定カテゴリと保存キーは次の対応です。ウィンドウ状態は
 * {@code window.*}、Git実行ファイルは {@code git.executable}、開く
 * ダイアログの基準フォルダーは {@code open.baseDirectory}、最近の
 * リポジトリは {@code recent.repositories}、テーマは {@code darkMode} です。
 * このクラスはカテゴリ値の検証と、既存ファイル形式の永続化だけを担当し、
 * JavaFXのテーマ適用は {@code ThemeService} に委譲します。</p>
 */
public final class ApplicationSettings {

    private static final String FILE_NAME = "settings.properties";
    private static final String DARK_MODE = "darkMode";
    private static final String WINDOW_WIDTH = "window.width";
    private static final String WINDOW_HEIGHT = "window.height";
    private static final String WINDOW_X = "window.x";
    private static final String WINDOW_Y = "window.y";
    private static final String GIT_EXECUTABLE = "git.executable";
    private static final String OPEN_BASE_DIRECTORY = "open.baseDirectory";
    private static final String RECENT_REPOSITORIES = "recent.repositories";
    private final Path path;
    private final List<String> recentRepositories;
    private boolean darkMode;
    private double windowWidth;
    private double windowHeight;
    private double windowX;
    private double windowY;
    private String gitExecutable;
    private Path openBaseDirectory;

    private ApplicationSettings(Path path) {
        this.path = path;
        darkMode = SettingsDefaults.DARK_MODE;
        windowWidth = SettingsDefaults.WINDOW_WIDTH;
        windowHeight = SettingsDefaults.WINDOW_HEIGHT;
        windowX = SettingsDefaults.WINDOW_POSITION;
        windowY = SettingsDefaults.WINDOW_POSITION;
        gitExecutable = SettingsDefaults.GIT_EXECUTABLE;
        openBaseDirectory = null;
        recentRepositories = new ArrayList<>();
    }

    /**
     * ユーザー共通の設定を読み込みます。
     *
     * @return 読み込んだ設定
     * @throws IOException 設定ファイルを読み込めない場合
     */
    public static ApplicationSettings load() throws IOException {
        return load(Paths.get(System.getProperty("user.home"), ".git-gui-windows-app", FILE_NAME));
    }

    static ApplicationSettings load(Path path) throws IOException {
        ApplicationSettings settings = new ApplicationSettings(path);
        if (!Files.exists(path)) {
            return settings;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        settings.darkMode = Boolean.parseBoolean(properties.getProperty(DARK_MODE,
                Boolean.toString(SettingsDefaults.DARK_MODE)));
        settings.windowWidth = positiveOrDefault(properties, WINDOW_WIDTH, settings.windowWidth);
        settings.windowHeight = positiveOrDefault(properties, WINDOW_HEIGHT, settings.windowHeight);
        settings.windowX = finiteOrDefault(properties, WINDOW_X, settings.windowX);
        settings.windowY = finiteOrDefault(properties, WINDOW_Y, settings.windowY);
        settings.gitExecutable = properties.getProperty(GIT_EXECUTABLE,
                SettingsDefaults.GIT_EXECUTABLE).trim();
        if (settings.gitExecutable.isEmpty()) {
            settings.gitExecutable = SettingsDefaults.GIT_EXECUTABLE;
        }
        String openBaseDirectory = properties.getProperty(OPEN_BASE_DIRECTORY, "").trim();
        if (!openBaseDirectory.isEmpty()) {
            try {
                settings.openBaseDirectory = Path.of(openBaseDirectory).toAbsolutePath().normalize();
            } catch (InvalidPathException e) {
                settings.openBaseDirectory = null;
            }
        }
        String recent = properties.getProperty(RECENT_REPOSITORIES, "");
        for (String value : recent.split("\\|", -1)) {
            if (!value.isBlank() && !settings.recentRepositories.contains(value)) {
                settings.recentRepositories.add(value);
            }
            if (settings.recentRepositories.size() == SettingsDefaults.MAX_RECENT_REPOSITORIES) {
                break;
            }
        }
        return settings;
    }

    private static double positiveOrDefault(Properties properties, String key, double defaultValue) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, ""));
            return Double.isFinite(value) && value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double finiteOrDefault(Properties properties, String key, double defaultValue) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, ""));
            return Double.isFinite(value) ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 現在の設定を保存します。
     *
     * @throws IOException 設定ファイルを保存できない場合
     */
    public void save() throws IOException {
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("設定ファイルの親ディレクトリを決定できません: " + path);
        }
        Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty(DARK_MODE, Boolean.toString(darkMode));
        properties.setProperty(WINDOW_WIDTH, Double.toString(windowWidth));
        properties.setProperty(WINDOW_HEIGHT, Double.toString(windowHeight));
        if (Double.isFinite(windowX)) {
            properties.setProperty(WINDOW_X, Double.toString(windowX));
        }
        if (Double.isFinite(windowY)) {
            properties.setProperty(WINDOW_Y, Double.toString(windowY));
        }
        properties.setProperty(GIT_EXECUTABLE, gitExecutable);
        if (openBaseDirectory != null) {
            properties.setProperty(OPEN_BASE_DIRECTORY, openBaseDirectory.toString());
        }
        properties.setProperty(RECENT_REPOSITORIES, String.join("|", recentRepositories));
        try (Writer writer = Files.newBufferedWriter(path)) {
            properties.store(writer, "Git GUI Windows App settings");
        }
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(double windowWidth) {
        this.windowWidth = windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    public double getWindowX() {
        return windowX;
    }

    public void setWindowX(double windowX) {
        this.windowX = windowX;
    }

    public double getWindowY() {
        return windowY;
    }

    public void setWindowY(double windowY) {
        this.windowY = windowY;
    }

    /**
     * 設定ファイルの保存先を取得します。
     *
     * @return 設定ファイルのパス
     */
    public Path getPath() {
        return path;
    }

    public WindowSettings windowSettings() {
        return new WindowSettings(windowWidth, windowHeight, windowX, windowY);
    }

    public GitSettings gitSettings() {
        return new GitSettings(gitExecutable);
    }

    public RepositorySettings repositorySettings() {
        return new RepositorySettings(openBaseDirectory, recentRepositories);
    }

    public ThemeSettings themeSettings() {
        return new ThemeSettings(darkMode);
    }

    public String getGitExecutable() {
        return gitExecutable;
    }

    public void setGitExecutable(String gitExecutable) {
        if (gitExecutable != null && !gitExecutable.isBlank()) {
            this.gitExecutable = gitExecutable.trim();
        }
    }

    public Path getOpenBaseDirectory() {
        return openBaseDirectory;
    }

    public void setOpenBaseDirectory(Path openBaseDirectory) {
        this.openBaseDirectory = openBaseDirectory == null
                ? null
                : openBaseDirectory.toAbsolutePath().normalize();
    }

    public List<String> getRecentRepositories() {
        return List.copyOf(recentRepositories);
    }

    public void addRecentRepository(Path repository) {
        if (repository == null) {
            return;
        }
        String value = repository.toAbsolutePath().normalize().toString();
        recentRepositories.remove(value);
        recentRepositories.add(0, value);
        if (recentRepositories.size() > SettingsDefaults.MAX_RECENT_REPOSITORIES) {
            recentRepositories.subList(SettingsDefaults.MAX_RECENT_REPOSITORIES,
                    recentRepositories.size()).clear();
        }
    }
}
