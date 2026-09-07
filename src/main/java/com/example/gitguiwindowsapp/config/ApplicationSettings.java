package com.example.gitguiwindowsapp.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
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
 */
public final class ApplicationSettings {

    private static final String FILE_NAME = "settings.properties";
    private static final String DARK_MODE = "darkMode";
    private static final String WINDOW_WIDTH = "window.width";
    private static final String WINDOW_HEIGHT = "window.height";
    private static final String WINDOW_X = "window.x";
    private static final String WINDOW_Y = "window.y";
    private static final String GIT_EXECUTABLE = "git.executable";
    private static final String RECENT_REPOSITORIES = "recent.repositories";
    private static final int MAX_RECENT_REPOSITORIES = 5;

    private final Path path;
    private boolean darkMode;
    private double windowWidth;
    private double windowHeight;
    private double windowX;
    private double windowY;
    private String gitExecutable;
    private final List<String> recentRepositories;

    private ApplicationSettings(Path path) {
        this.path = path;
        darkMode = false;
        windowWidth = 800;
        windowHeight = 600;
        windowX = Double.NaN;
        windowY = Double.NaN;
        gitExecutable = "git";
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
        settings.darkMode = Boolean.parseBoolean(properties.getProperty(DARK_MODE, "false"));
        settings.windowWidth = positiveOrDefault(properties, WINDOW_WIDTH, settings.windowWidth);
        settings.windowHeight = positiveOrDefault(properties, WINDOW_HEIGHT, settings.windowHeight);
        settings.windowX = finiteOrDefault(properties, WINDOW_X, settings.windowX);
        settings.windowY = finiteOrDefault(properties, WINDOW_Y, settings.windowY);
        settings.gitExecutable = properties.getProperty(GIT_EXECUTABLE, "git").trim();
        if (settings.gitExecutable.isEmpty()) {
            settings.gitExecutable = "git";
        }
        String recent = properties.getProperty(RECENT_REPOSITORIES, "");
        for (String value : recent.split("\\|", -1)) {
            if (!value.isBlank() && !settings.recentRepositories.contains(value)) {
                settings.recentRepositories.add(value);
            }
            if (settings.recentRepositories.size() == MAX_RECENT_REPOSITORIES) {
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
        Files.createDirectories(path.getParent());
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

    public String getGitExecutable() {
        return gitExecutable;
    }

    public void setGitExecutable(String gitExecutable) {
        if (gitExecutable != null && !gitExecutable.isBlank()) {
            this.gitExecutable = gitExecutable.trim();
        }
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
        if (recentRepositories.size() > MAX_RECENT_REPOSITORIES) {
            recentRepositories.subList(MAX_RECENT_REPOSITORIES, recentRepositories.size()).clear();
        }
    }
}
