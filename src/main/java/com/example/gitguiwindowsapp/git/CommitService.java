package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.Objects;

/**
 * コミット実行を担当します。
 */
public final class CommitService {
    private final GitService gitService;

    public CommitService(GitService gitService) {
        this.gitService = Objects.requireNonNull(gitService, "gitService");
    }

    public GitOperationResult commit(Path directory, String message) throws GitCommandException {
        return gitService.commit(directory, message);
    }
}
