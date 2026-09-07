package com.example.gitguiwindowsapp.git;

import org.junit.jupiter.api.Test;

import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitServiceTest {
    @Test
    void validatesRepositoryAndReturnsItsRoot() throws Exception {
        Path directory = Files.createTempDirectory("git-service");
        try {
            new GitCommandRunner().run(directory, java.util.List.of("init", "-q"));
            Path root = new GitService().validateRepository(directory);
            assertEquals(directory.toAbsolutePath().normalize(), root);
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void rejectsNonRepository() throws Exception {
        Path directory = Files.createTempDirectory("not-a-git-repository");
        try {
            assertThrows(GitCommandException.class,
                    () -> new GitService().validateRepository(directory));
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void stagesUnstagesCommitsAndCreatesBranches() throws Exception {
        Path directory = Files.createTempDirectory("git-service-operations");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            runner.run(directory, List.of("config", "user.email", "test@example.com"));
            runner.run(directory, List.of("config", "user.name", "Test User"));
            Files.writeString(directory.resolve("file.txt"), "one\n");
            GitService service = new GitService(runner);

            assertEquals(com.example.gitguiwindowsapp.model.FileChangeType.UNTRACKED,
                    service.status(directory).changes().get(0).type());
            service.stage(directory, "file.txt");
            assertEquals(com.example.gitguiwindowsapp.model.StageState.STAGED,
                    service.status(directory).changes().get(0).stageState());
            service.unstage(directory, "file.txt");
            assertEquals(com.example.gitguiwindowsapp.model.StageState.UNTRACKED,
                    service.status(directory).changes().get(0).stageState());
            service.stage(directory, "file.txt");
            assertEquals(40, service.commit(directory, "initial commit").commitId().length());
            service.createBranch(directory, "feature");
            service.checkout(directory, "feature");
            assertEquals("feature", service.status(directory).branch());
        } finally {
            deleteTree(directory);
        }
    }

    private static void deleteTree(Path directory) throws Exception {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    for (int attempt = 0; attempt < 5; attempt++) {
                        try {
                            DosFileAttributeView attributes =
                                    Files.getFileAttributeView(path, DosFileAttributeView.class);
                            if (attributes != null) {
                                attributes.setReadOnly(false);
                            }
                            Files.deleteIfExists(path);
                            return;
                        } catch (java.io.IOException e) {
                            if (attempt == 4) {
                                throw new RuntimeException(e);
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(interrupted);
                            }
                        }
                    }
                });
            }
        }
    }
}
