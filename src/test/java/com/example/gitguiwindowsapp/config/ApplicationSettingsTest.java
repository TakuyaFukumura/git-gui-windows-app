package com.example.gitguiwindowsapp.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            source.setOpenBaseDirectory(directory.resolve("repositories"));
            source.save();

            ApplicationSettings result = ApplicationSettings.load(file);

            assertTrue(result.isDarkMode());
            assertEquals(1024, result.getWindowWidth());
            assertEquals(768, result.getWindowHeight());
            assertEquals(120, result.getWindowX());
            assertEquals(80, result.getWindowY());
            assertEquals(directory.resolve("repositories").toAbsolutePath().normalize(),
                    result.getOpenBaseDirectory());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void invalidOpenBaseDirectoryUsesDefault() throws Exception {
        Path directory = Files.createTempDirectory("invalid-open-base-settings");
        Path file = directory.resolve("settings.properties");
        try {
            Files.writeString(file, "open.baseDirectory=bad\0path\n");

            ApplicationSettings result = ApplicationSettings.load(file);

            assertEquals(null, result.getOpenBaseDirectory());
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

    @Test
    void savesGitExecutableAndKeepsFiveRecentRepositories() throws Exception {
        Path directory = Files.createTempDirectory("git-settings");
        Path file = directory.resolve("settings.properties");
        try {
            ApplicationSettings settings = ApplicationSettings.load(file);
            settings.setGitExecutable("C:\\Git\\bin\\git.exe");
            for (int i = 0; i < 6; i++) {
                settings.addRecentRepository(directory.resolve("repo-" + i));
            }
            settings.save();

            ApplicationSettings result = ApplicationSettings.load(file);

            assertEquals("C:\\Git\\bin\\git.exe", result.getGitExecutable());
            assertEquals(5, result.getRecentRepositories().size());
            assertEquals(directory.resolve("repo-5").toAbsolutePath().normalize().toString(),
                    result.getRecentRepositories().get(0));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void removesDuplicateRecentRepositoriesWhilePreservingNewestOrder() throws Exception {
        Path directory = Files.createTempDirectory("duplicate-recent-settings");
        Path file = directory.resolve("settings.properties");
        try {
            Files.writeString(file, "recent.repositories=repo-a|repo-b|repo-a|repo-c\n");

            ApplicationSettings result = ApplicationSettings.load(file);

            assertEquals(java.util.List.of("repo-a", "repo-b", "repo-c"),
                    result.getRecentRepositories());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void propagatesSaveFailureWhenParentPathIsNotDirectory() throws Exception {
        Path directory = Files.createTempDirectory("settings-save-failure");
        Path parentFile = directory.resolve("not-a-directory");
        Files.writeString(parentFile, "occupied");
        try {
            ApplicationSettings settings = ApplicationSettings.load(parentFile.resolve("settings.properties"));

            assertThrows(java.nio.file.FileAlreadyExistsException.class, settings::save);
        } finally {
            Files.deleteIfExists(parentFile);
            Files.deleteIfExists(directory);
        }
    }
}
