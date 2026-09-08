package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.DiffDocument;
import com.example.gitguiwindowsapp.model.FileChange;
import com.example.gitguiwindowsapp.model.GitOperationResult;
import com.example.gitguiwindowsapp.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Git operations used by the UI. Every path supplied by a caller is passed as
 * a separate argument after {@code --}; no shell command string is assembled.
 */
public final class GitService {
    private final GitCommandRunner runner;
    private final GitStatusParser statusParser;
    private final GitDiffParser diffParser;
    private final GitBranchParser branchParser;

    public GitService() {
        this(new GitCommandRunner());
    }

    public GitService(GitCommandRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
        statusParser = new GitStatusParser();
        diffParser = new GitDiffParser();
        branchParser = new GitBranchParser();
    }

    public String gitVersion() throws GitCommandException {
        GitCommandResult result = execute(Path.of("."), "git --version",
                List.of("--version"));
        return result.standardOutput().trim();
    }

    public Path validateRepository(Path directory) throws GitCommandException {
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

    public RepositoryInfo status(Path directory) throws GitCommandException {
        Path root = validateRepository(directory);
        GitCommandResult result = execute(root, "Read status",
                List.of("status", "--porcelain=v1", "-z", "--branch"));
        GitStatusParser.ParsedStatus parsed = statusParser.parse(result.standardOutput());
        String head = "";
        GitCommandResult headResult = runnerCall(root, List.of("rev-parse", "--verify", "HEAD"));
        if (headResult.succeeded()) {
            head = headResult.standardOutput().trim();
        }
        return new RepositoryInfo(root, parsed.branch(), head, parsed.changes(),
                parsed.changes().isEmpty());
    }

    public DiffDocument diff(Path directory, String path, boolean cached) throws GitCommandException {
        Path root = validateRepository(directory);
        List<String> arguments = new ArrayList<>(List.of("diff", "--no-ext-diff", "--unified=3"));
        if (cached) {
            arguments.add("--cached");
        }
        if (path != null && !path.isBlank()) {
            arguments.add("--");
            arguments.add(path);
        }
        GitCommandResult result = execute(root, "Read diff", arguments);
        return diffParser.parse(result.standardOutput());
    }

    public GitOperationResult stage(Path directory, String path) throws GitCommandException {
        return operation(directory, "Stage file", List.of("add", "--", requirePath(path)));
    }

    public GitOperationResult unstage(Path directory, String path) throws GitCommandException {
        return operation(directory, "Unstage file",
                List.of("reset", "--", requirePath(path)));
    }

    public GitOperationResult commit(Path directory, String message) throws GitCommandException {
        if (message == null || message.isBlank()) {
            throw new GitCommandException("Commit", -1, "Commit message must not be empty.");
        }
        GitOperationResult result = operation(directory, "Commit",
                List.of("commit", "-m", message.trim()));
        if (result.success()) {
            GitCommandResult head = runnerCall(validateRepository(directory),
                    List.of("rev-parse", "--verify", "HEAD"));
            if (head.succeeded()) {
                return new GitOperationResult(true, result.exitCode(), result.output(),
                        result.error(), head.standardOutput().trim());
            }
        }
        return result;
    }

    public List<BranchInfo> branches(Path directory) throws GitCommandException {
        Path root = validateRepository(directory);
        GitCommandResult allBranches = execute(root, "List branches",
                List.of("branch", "--format=%(refname:short)%09%(HEAD)"));
        GitCommandResult mergedBranches = execute(root, "List merged branches",
                List.of("branch", "--format=%(refname:short)", "--merged"));
        Set<String> merged = new HashSet<>(mergedBranches.standardOutput().lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList());
        return branchParser.parse(allBranches.standardOutput()).stream()
                .map(branch -> new BranchInfo(branch.name(), branch.current(),
                        merged.contains(branch.name())))
                .toList();
    }

    public List<String> commitGraph(Path directory) throws GitCommandException {
        Path root = validateRepository(directory);
        GitCommandResult result = execute(root, "Read commit history",
                List.of("log", "--graph", "--all", "--decorate", "--date=short",
                        "--pretty=format:%h%x09%ad%x09%an%x09%s"));
        return result.standardOutput().lines().toList();
    }

    public GitOperationResult createBranch(Path directory, String name) throws GitCommandException {
        Path root = validateRepository(directory);
        validateBranchName(root, name);
        return operation(root, "Create branch", List.of("branch", name.trim()));
    }

    public GitOperationResult checkout(Path directory, String name) throws GitCommandException {
        Path root = validateRepository(directory);
        validateBranchName(root, name);
        return operation(root, "Switch branch", List.of("switch", name.trim()));
    }

    public GitOperationResult deleteBranch(Path directory, String name) throws GitCommandException {
        Path root = validateRepository(directory);
        validateBranchName(root, name);
        return operation(root, "Delete branch", List.of("branch", "-d", name.trim()));
    }

    private GitOperationResult operation(Path directory, String operation, List<String> arguments)
            throws GitCommandException {
        Path root = validateRepository(directory);
        GitCommandResult result = execute(root, operation, arguments);
        return new GitOperationResult(result.succeeded(), result.exitCode(),
                result.standardOutput(), result.standardError());
    }

    private GitCommandResult execute(Path directory, String operation, List<String> arguments)
            throws GitCommandException {
        GitCommandResult result = runnerCall(directory, arguments);
        if (!result.succeeded()) {
            throw new GitCommandException(operation, result.exitCode(), result.standardError());
        }
        return result;
    }

    private GitCommandResult runnerCall(Path directory, List<String> arguments)
            throws GitCommandException {
        try {
            return runner.run(directory, arguments);
        } catch (IOException e) {
            throw new GitCommandException("Git execution", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Git execution", "Git command was interrupted.", e);
        }
    }

    private static String requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank.");
        }
        return path;
    }

    private void validateBranchName(Path repository, String name) throws GitCommandException {
        if (name == null || name.isBlank()) {
            throw new GitCommandException("Branch validation", -1,
                    "Invalid branch name: " + name);
        }
        GitCommandResult result = runnerCall(repository,
                List.of("check-ref-format", "--branch", name.trim()));
        if (!result.succeeded()) {
            throw new GitCommandException("Branch validation", result.exitCode(),
                    result.standardError());
        }
    }
}
