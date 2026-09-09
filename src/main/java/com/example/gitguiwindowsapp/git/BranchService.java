package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ローカルブランチの一覧・作成・切替・削除を担当します。
 */
public final class BranchService {
    private final GitCommandSupport commands;
    private final GitBranchParser branchParser = new GitBranchParser();

    public BranchService() {
        this(new GitCommandRunner());
    }

    public BranchService(GitCommandExecutor runner) {
        commands = new GitCommandSupport(runner);
    }

    public List<BranchInfo> list(Path directory) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        GitCommandResult allBranches = commands.execute(root, "List branches",
                List.of("branch", "--format=%(refname:short)%09%(HEAD)"));
        GitCommandResult mergedBranches = commands.execute(root, "List merged branches",
                List.of("branch", "--format=%(refname:short)", "--merged"));
        Set<String> merged = new HashSet<>(mergedBranches.standardOutput().lines()
                .map(String::trim).filter(line -> !line.isBlank()).toList());
        return branchParser.parse(allBranches.standardOutput()).stream()
                .map(branch -> new BranchInfo(branch.name(), branch.current(),
                        merged.contains(branch.name()))).toList();
    }

    public GitOperationResult create(Path directory, String name) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        commands.validateBranchName(root, name);
        return commands.operation(root, "Create branch", List.of("branch", name.trim()));
    }

    public GitOperationResult checkout(Path directory, String name) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        commands.validateBranchName(root, name);
        return commands.operation(root, "Switch branch", List.of("switch", name.trim()));
    }

    public GitOperationResult delete(Path directory, String name) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        commands.validateBranchName(root, name);
        return commands.operation(root, "Delete branch", List.of("branch", "-d", name.trim()));
    }
}
