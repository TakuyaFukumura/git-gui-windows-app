package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.RepositoryInfo;

import java.nio.file.Path;
import java.util.Objects;

/**
 * リポジトリの検証と状態取得を担当します。
 */
public final class RepositoryService {
    private final GitService gitService;

    public RepositoryService(GitService gitService) {
        this.gitService = Objects.requireNonNull(gitService, "gitService");
    }

    public String gitVersion() throws GitCommandException {
        return gitService.gitVersion();
    }

    public Path validate(Path directory) throws GitCommandException {
        return gitService.validateRepository(directory);
    }

    public RepositoryInfo status(Path directory) throws GitCommandException {
        return gitService.status(directory);
    }
}
