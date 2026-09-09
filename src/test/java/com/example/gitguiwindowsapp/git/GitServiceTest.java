package com.example.gitguiwindowsapp.git;

import org.junit.jupiter.api.Test;

import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitServiceTest {
    @Test
    void validatesRepositoryAndReturnsItsRoot() throws Exception {
        Path directory = Files.createTempDirectory("git-service");
        try {
            new GitCommandRunner().run(directory, java.util.List.of("init", "-q"));
            Path root = new GitService().validateRepository(directory);
            assertEquals(directory.toRealPath(), root);
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

    @Test
    void rejectsInvalidBranchNamesUsingGitRefRules() throws Exception {
        Path directory = Files.createTempDirectory("git-service-branch-validation");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            GitService service = new GitService(runner);

            assertThrows(GitCommandException.class,
                    () -> service.createBranch(directory, "invalid..name"));
            assertThrows(GitCommandException.class,
                    () -> service.createBranch(directory, "-invalid"));
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void deletesOnlyMergedBranches() throws Exception {
        Path directory = Files.createTempDirectory("git-service-delete-branch");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            runner.run(directory, List.of("config", "user.email", "test@example.com"));
            runner.run(directory, List.of("config", "user.name", "Test User"));
            Files.writeString(directory.resolve("file.txt"), "one\n");
            runner.run(directory, List.of("add", "file.txt"));
            runner.run(directory, List.of("commit", "-qm", "initial"));
            GitService service = new GitService(runner);
            String defaultBranch = runner.run(directory, List.of("branch", "--show-current"))
                    .standardOutput().trim();
            service.createBranch(directory, "merged");
            service.createBranch(directory, "unmerged");
            service.checkout(directory, "unmerged");
            Files.writeString(directory.resolve("file.txt"), "two\n");
            service.stage(directory, "file.txt");
            service.commit(directory, "unmerged change");
            service.checkout(directory, defaultBranch);
            service.deleteBranch(directory, "merged");
            assertThrows(GitCommandException.class,
                    () -> service.deleteBranch(directory, "unmerged"));
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void listsBranchesAndMarksMergedBranches() throws Exception {
        Path directory = Files.createTempDirectory("git-service-list-branches");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            runner.run(directory, List.of("config", "user.email", "test@example.com"));
            runner.run(directory, List.of("config", "user.name", "Test User"));
            Files.writeString(directory.resolve("file.txt"), "one\n");
            runner.run(directory, List.of("add", "file.txt"));
            runner.run(directory, List.of("commit", "-qm", "initial"));

            GitService service = new GitService(runner);
            String defaultBranch = runner.run(directory, List.of("branch", "--show-current"))
                    .standardOutput().trim();
            service.createBranch(directory, "feature");

            List<com.example.gitguiwindowsapp.model.BranchInfo> branches =
                    service.branches(directory);

            assertEquals(2, branches.size());
            List<String> branchNames = branches.stream()
                    .map(com.example.gitguiwindowsapp.model.BranchInfo::name).toList();
            assertTrue(branchNames.contains(defaultBranch));
            assertTrue(branchNames.contains("feature"));
            assertEquals(2, branches.stream().filter(com.example.gitguiwindowsapp.model.BranchInfo::merged)
                    .count());
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    void listsCommitGraphAcrossBranches() throws Exception {
        Path directory = Files.createTempDirectory("git-service-commit-graph");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            runner.run(directory, List.of("config", "user.email", "test@example.com"));
            runner.run(directory, List.of("config", "user.name", "Test User"));
            Files.writeString(directory.resolve("file.txt"), "one\n");
            runner.run(directory, List.of("add", "file.txt"));
            runner.run(directory, List.of("commit", "-qm", "initial"));

            List<com.example.gitguiwindowsapp.model.CommitEntry> graph =
                    new GitService(runner).commitHistory(directory);

            assertEquals(1, graph.size());
            assertEquals("initial", graph.get(0).subject());
            assertEquals(40, graph.get(0).id().length());
            assertTrue(graph.get(0).graph().stream()
                    .anyMatch(segment -> segment.lane() == 0));
        } finally {
            deleteTree(directory);
        }

    }

    @Test
    void classifiesCommitReferences() throws Exception {
        Path directory = Files.createTempDirectory("git-service-commit-references");
        try {
            GitCommandRunner runner = new GitCommandRunner();
            runner.run(directory, List.of("init", "-q"));
            runner.run(directory, List.of("config", "user.email", "test@example.com"));
            runner.run(directory, List.of("config", "user.name", "Test User"));
            Files.writeString(directory.resolve("file.txt"), "one\n");
            runner.run(directory, List.of("add", "file.txt"));
            runner.run(directory, List.of("commit", "-qm", "initial"));
            runner.run(directory, List.of("branch", "feature"));
            runner.run(directory, List.of("tag", "v1.0.0"));

            var entry = new GitService(runner).commitHistory(directory).get(0);

            assertTrue(entry.references().stream().anyMatch(reference ->
                    reference.type() == com.example.gitguiwindowsapp.model.CommitReferenceType.HEAD));
            assertTrue(entry.references().stream().anyMatch(reference ->
                    reference.type() == com.example.gitguiwindowsapp.model.CommitReferenceType.LOCAL_BRANCH
                            && reference.name().equals("feature")));
            assertTrue(entry.references().stream().anyMatch(reference ->
                    reference.type() == com.example.gitguiwindowsapp.model.CommitReferenceType.TAG
                            && reference.name().equals("v1.0.0")));
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
