package com.example.gitguiwindowsapp.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Comparator;
import java.util.List;

/**
 * Gitサービステストで共有する一時リポジトリFixtureです。
 */
final class GitRepositoryFixture implements AutoCloseable {
    private final Path directory;
    private final GitCommandRunner runner = new GitCommandRunner();

    private GitRepositoryFixture(Path directory) {
        this.directory = directory;
    }

    static GitRepositoryFixture create() throws Exception {
        Path directory = Files.createTempDirectory("git-fixture");
        GitRepositoryFixture fixture = new GitRepositoryFixture(directory);
        fixture.run("init", "-q");
        fixture.run("config", "user.email", "test@example.com");
        fixture.run("config", "user.name", "Test User");
        return fixture;
    }

    Path directory() {
        return directory;
    }

    GitCommandResult run(String... arguments) throws Exception {
        return runner.run(directory, List.of(arguments));
    }

    @Override
    public void close() throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    DosFileAttributeView attributes =
                            Files.getFileAttributeView(path, DosFileAttributeView.class);
                    if (attributes != null) {
                        attributes.setReadOnly(false);
                    }
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
