package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * ローカルブランチの一覧・作成・切替・削除を担当します。
 */
public final class BranchService {
    private final GitService gitService;

    public BranchService(GitService gitService) {
        this.gitService = Objects.requireNonNull(gitService, "gitService");
    }

    public List<BranchInfo> list(Path directory) throws GitCommandException {
        return gitService.branches(directory);
    }

    public GitOperationResult create(Path directory, String name) throws GitCommandException {
        return gitService.createBranch(directory, name);
    }

    public GitOperationResult checkout(Path directory, String name) throws GitCommandException {
        return gitService.checkout(directory, name);
    }

    public GitOperationResult delete(Path directory, String name) throws GitCommandException {
        return gitService.deleteBranch(directory, name);
    }
}
