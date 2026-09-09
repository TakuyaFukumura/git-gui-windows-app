package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Gitコマンド実行に共通する検証とエラー処理を提供します。
 */
final class GitCommandSupport {
    private final GitCommandExecutor runner;

    GitCommandSupport(GitCommandExecutor runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    Path validateRepository(Path directory) throws GitCommandException {
        Path candidate = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        if (!Files.isDirectory(candidate)) {
            throw new GitCommandException("Repository validation", -1,
                    "Directory does not exist: " + candidate);
        }
        GitCommandResult result = execute(candidate, "Repository validation",
                List.of("rev-parse", "--show-toplevel"));
        try {
            return Path.of(result.standardOutput().trim()).toRealPath();
        } catch (IOException e) {
            throw new GitCommandException("Repository validation",
                    "Git returned an inaccessible repository root.", e);
        }
    }

    GitCommandResult execute(Path directory, String operation, List<String> arguments)
            throws GitCommandException {
        GitCommandResult result = run(directory, arguments);
        if (!result.succeeded()) {
            throw new GitCommandException(operation, result.exitCode(), result.standardError());
        }
        return result;
    }

    GitCommandResult run(Path directory, List<String> arguments) throws GitCommandException {
        try {
            return runner.run(directory, arguments);
        } catch (IOException e) {
            throw new GitCommandException("Git execution", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Git execution", "Git command was interrupted.", e);
        }
    }

    GitOperationResult operation(Path directory, String operation, List<String> arguments)
            throws GitCommandException {
        Path root = validateRepository(directory);
        GitCommandResult result = execute(root, operation, arguments);
        return new GitOperationResult(result.succeeded(), result.exitCode(),
                result.standardOutput(), result.standardError());
    }

    static String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank.");
        }
        return path;
    }

    void validateBranchName(Path repository, String name) throws GitCommandException {
        if (name == null || name.isBlank()) {
            throw new GitCommandException("Branch validation", -1,
                    "Invalid branch name: " + name);
        }
        GitCommandResult result = run(repository,
                List.of("check-ref-format", "--branch", name.trim()));
        if (!result.succeeded()) {
            throw new GitCommandException("Branch validation", result.exitCode(),
                    result.standardError());
        }
    }
}
