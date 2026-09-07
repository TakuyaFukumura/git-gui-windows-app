package com.example.basicwindowsapp.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupServiceTest {

    @Test
    void backupRoundTripRestoresDatabaseAndSettings() throws Exception {
        Path directory = Files.createTempDirectory("basic-windows-backup");
        try {
            Path database = directory.resolve("source.db");
            Path settings = directory.resolve("source.properties");
            Path archive = directory.resolve("backup.bwa");
            Path restoredDatabase = directory.resolve("restored.db");
            Path restoredSettings = directory.resolve("restored.properties");
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE messages (id INTEGER PRIMARY KEY, text TEXT NOT NULL, created_at INTEGER NOT NULL)");
                statement.execute("INSERT INTO messages VALUES (1, 'database', 123)");
            }
            Files.writeString(settings, "darkMode=true");

            BackupService.createBackup(archive, database, settings);
            BackupService.restoreBackup(archive, restoredDatabase, restoredSettings);

            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + restoredDatabase);
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT text FROM messages")) {
                result.next();
                assertEquals("database", result.getString(1));
            }
            assertEquals("darkMode=true", Files.readString(restoredSettings));
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted((left, right) -> right.compareTo(left)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
