package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.CommitEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * コミット履歴と参照情報の取得を担当します。
 */
public final class HistoryService {
    private final GitService gitService;

    public HistoryService(GitService gitService) {
        this.gitService = Objects.requireNonNull(gitService, "gitService");
    }

    public List<CommitEntry> list(Path directory) throws GitCommandException {
        return gitService.commitHistory(directory);
    }
}
