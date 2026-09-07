package com.example.basicwindowsapp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * アプリケーションデータのバックアップと復元を提供します。
 */
public final class BackupService {

    private static final String DATABASE_ENTRY = "basicwindowsapp.db";
    private static final String SETTINGS_ENTRY = "settings.properties";
    private static final long MAX_DATABASE_SIZE = 100 * 1024 * 1024;
    private static final long MAX_SETTINGS_SIZE = 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = MAX_DATABASE_SIZE + MAX_SETTINGS_SIZE;
    private static final int MAX_ENTRIES = 2;
    private static final int MAX_COMPRESSION_RATIO = 1000;

    private BackupService() {
    }

    /**
     * データベースと設定を1つのZIPファイルへ保存します。
     *
     * @param archive  出力先
     * @param database データベースファイル
     * @param settings 設定ファイル
     * @throws IOException 入出力に失敗した場合
     */
    public static void createBackup(Path archive, Path database, Path settings) throws IOException {
        Files.createDirectories(database.toAbsolutePath().getParent());
        Path snapshot = Files.createTempFile(database.toAbsolutePath().getParent(), "backup-", ".db");
        try {
            Files.deleteIfExists(snapshot);
            createDatabaseSnapshot(database, snapshot);
            try (OutputStream output = Files.newOutputStream(archive);
                 ZipOutputStream zip = new ZipOutputStream(output)) {
                addFile(zip, DATABASE_ENTRY, snapshot);
                if (Files.exists(settings)) {
                    addFile(zip, SETTINGS_ENTRY, settings);
                }
            }
        } finally {
            Files.deleteIfExists(snapshot);
        }
    }

    /**
     * ZIPバックアップからデータベースと設定を復元します。
     *
     * @param archive  入力元
     * @param database 復元先データベース
     * @param settings 復元先設定ファイル
     * @throws IOException 入出力または形式検証に失敗した場合
     */
    public static void restoreBackup(Path archive, Path database, Path settings) throws IOException {
        Path databaseParent = database.toAbsolutePath().getParent();
        Path settingsParent = settings.toAbsolutePath().getParent();
        Files.createDirectories(databaseParent);
        Files.createDirectories(settingsParent);
        Path temporaryDirectory = Files.createTempDirectory(databaseParent, "restore-");
        Set<String> restored = new HashSet<>();
        long totalSize = 0;
        try {
            try (InputStream input = Files.newInputStream(archive);
                 ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                int entryCount = 0;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entryCount > MAX_ENTRIES || entry.isDirectory() || !restored.add(entry.getName())) {
                        throw new IOException("バックアップのエントリ構成が正しくありません。");
                    }
                    long maxSize;
                    if (DATABASE_ENTRY.equals(entry.getName())) {
                        maxSize = MAX_DATABASE_SIZE;
                    } else if (SETTINGS_ENTRY.equals(entry.getName())) {
                        maxSize = MAX_SETTINGS_SIZE;
                    } else {
                        throw new IOException("バックアップに未対応のファイルが含まれています。");
                    }
                    if (entry.getSize() > maxSize || entry.getCompressedSize() > 0
                            && entry.getSize() > entry.getCompressedSize() * MAX_COMPRESSION_RATIO) {
                        throw new IOException("バックアップのファイルサイズが許容範囲を超えています。");
                    }
                    Path destination = temporaryDirectory.resolve(entry.getName());
                    long extracted = 0;
                    try (OutputStream output = Files.newOutputStream(destination,
                            StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            extracted += read;
                            totalSize += read;
                            if (extracted > maxSize || totalSize > MAX_TOTAL_SIZE) {
                                throw new IOException("バックアップの展開サイズが許容範囲を超えています。");
                            }
                            output.write(buffer, 0, read);
                        }
                    }
                }
            }
            if (!restored.contains(DATABASE_ENTRY)) {
                throw new IOException("バックアップにデータベースが含まれていません。");
            }
            Path temporaryDatabase = temporaryDirectory.resolve(DATABASE_ENTRY);
            Path temporarySettings = temporaryDirectory.resolve(SETTINGS_ENTRY);
            validateDatabase(temporaryDatabase);
            if (Files.exists(temporarySettings)) {
                validateSettings(temporarySettings);
            }
            replaceAtomically(temporaryDatabase, database);
            if (Files.exists(temporarySettings)) {
                replaceAtomically(temporarySettings, settings);
            }
        } finally {
            deleteDirectory(temporaryDirectory);
        }
    }

    private static void createDatabaseSnapshot(Path database, Path snapshot) throws IOException {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + snapshot.toAbsolutePath().toString().replace("'", "''") + "'");
        } catch (SQLException e) {
            throw new IOException("SQLiteデータベースのスナップショット作成に失敗しました。", e);
        }
    }

    private static void validateDatabase(Path database) throws IOException {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA integrity_check")) {
            if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                throw new IOException("バックアップのデータベース検証に失敗しました。");
            }
        } catch (SQLException e) {
            throw new IOException("バックアップのデータベースを読み込めません。", e);
        }
    }

    private static void validateSettings(Path settings) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(settings)) {
            properties.load(reader);
        } catch (IllegalArgumentException e) {
            throw new IOException("バックアップの設定ファイルが壊れています。", e);
        }
    }

    private static void replaceAtomically(Path source, Path target) throws IOException {
        Path backup = target.resolveSibling(target.getFileName() + ".restore-backup");
        Files.deleteIfExists(backup);
        boolean existed = Files.exists(target);
        if (existed) {
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            Files.deleteIfExists(backup);
        } catch (IOException e) {
            Files.deleteIfExists(target);
            if (existed) {
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            for (Path path : files.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void addFile(ZipOutputStream zip, String entryName, Path source) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("バックアップ対象が存在しません: " + source);
        }
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(source, zip);
        zip.closeEntry();
    }
}
