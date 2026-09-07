package com.example.gitguiwindowsapp.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationSettingsTest {

    @Test
    void savesAndLoadsWindowAndThemeSettings() throws Exception {
        Path directory = Files.createTempDirectory("basic-windows-settings");
        Path file = directory.resolve("settings.properties");
        try {
            ApplicationSettings source = ApplicationSettings.load(file);
            source.setDarkMode(true);
            source.setWindowWidth(1024);
            source.setWindowHeight(768);
            source.setWindowX(120);
            source.setWindowY(80);
            source.save();

            ApplicationSettings result = ApplicationSettings.load(file);

            assertTrue(result.isDarkMode());
            assertEquals(1024, result.getWindowWidth());
            assertEquals(768, result.getWindowHeight());
            assertEquals(120, result.getWindowX());
            assertEquals(80, result.getWindowY());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void invalidValuesUseDefaults() throws Exception {
        Path directory = Files.createTempDirectory("basic-windows-settings");
        Path file = directory.resolve("settings.properties");
        try {
            Files.writeString(file, "window.width=-1\nwindow.height=invalid\nwindow.x=NaN\n");

            ApplicationSettings result = ApplicationSettings.load(file);

            assertEquals(800, result.getWindowWidth());
            assertEquals(600, result.getWindowHeight());
            assertTrue(Double.isNaN(result.getWindowX()));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
