package com.example.gitguiwindowsapp.git;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitCommandRunnerTest {
    @Test
    void executesGitWithoutAWrapperShell() throws Exception {
        GitCommandResult result = new GitCommandRunner().run(Path.of("."), List.of("--version"));
        assertTrue(result.succeeded());
        assertTrue(result.standardOutput().startsWith("git version"));
    }

    @Test
    void servicesAcceptAReplaceableGitExecutionBoundary() throws Exception {
        Path directory = Path.of(".").toAbsolutePath().normalize();
        GitCommandExecutor fake = (workingDirectory, arguments) -> new GitCommandResult(
                0, directory + System.lineSeparator(), "", 0);

        assertEquals(directory.toRealPath(),
                new RepositoryService(fake).validate(directory));
    }

    @Test
    void sharedFixtureCreatesAConfiguredRepository() throws Exception {
        try (GitRepositoryFixture fixture = GitRepositoryFixture.create()) {
            assertEquals("true", fixture.run("rev-parse", "--is-inside-work-tree")
                    .standardOutput().trim());
        }
    }
}
