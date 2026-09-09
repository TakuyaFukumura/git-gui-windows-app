package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.DiffDocument;
import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 変更一覧の差分表示とステージ操作を担当します。
 */
public final class ChangeService {
    private final GitService gitService;

    public ChangeService(GitService gitService) {
        this.gitService = Objects.requireNonNull(gitService, "gitService");
    }

    public DiffDocument diff(Path directory, String path, boolean cached) throws GitCommandException {
        return gitService.diff(directory, path, cached);
    }

    public GitOperationResult stage(Path directory, String path) throws GitCommandException {
        return gitService.stage(directory, path);
    }

    public GitOperationResult unstage(Path directory, String path) throws GitCommandException {
        return gitService.unstage(directory, path);
    }
}
