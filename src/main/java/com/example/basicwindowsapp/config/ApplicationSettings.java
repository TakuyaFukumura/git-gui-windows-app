package com.example.basicwindowsapp.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final Path path;
    private boolean darkMode;
    private double windowWidth;
    private double windowHeight;
    private double windowX;
    private double windowY;

    private ApplicationSettings(Path path) {
        this.path = path;
        darkMode = false;
        windowWidth = 800;
        windowHeight = 600;
        windowX = Double.NaN;
        windowY = Double.NaN;
    }

    /**
     * ユーザー共通の設定を読み込みます。
     *
     * @return 読み込んだ設定
     * @throws IOException 設定ファイルを読み込めない場合
     */
    public static ApplicationSettings load() throws IOException {
        return load(Paths.get(System.getProperty("user.home"), ".basic-windows-app", FILE_NAME));
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
        try (Writer writer = Files.newBufferedWriter(path)) {
            properties.store(writer, "Basic Windows App settings");
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
}
