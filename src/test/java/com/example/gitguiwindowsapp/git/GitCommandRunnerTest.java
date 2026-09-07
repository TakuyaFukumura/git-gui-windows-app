package com.example.gitguiwindowsapp.git;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitCommandRunnerTest {
    @Test
    void executesGitWithoutAWrapperShell() throws Exception {
        GitCommandResult result = new GitCommandRunner().run(Path.of("."), List.of("--version"));
        assertTrue(result.succeeded());
        assertTrue(result.standardOutput().startsWith("git version"));
    }
}
