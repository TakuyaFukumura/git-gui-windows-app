package com.example.gitguiwindowsapp.application;

import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.RepositoryInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * JavaFXコントロールに依存しない画面状態を保持します。
 */
public final class AppState {
    private Path repository;
    private RepositoryInfo repositoryInfo;
    private List<BranchInfo> branches = List.of();
    private List<CommitEntry> history = List.of();
    private String historyQuery = "";
    private boolean operationRunning;

    public Path repository() {
        return repository;
    }

    public RepositoryInfo repositoryInfo() {
        return repositoryInfo;
    }

    public List<BranchInfo> branches() {
        return branches;
    }

    public List<CommitEntry> history() {
        return history;
    }

    public String historyQuery() {
        return historyQuery;
    }

    public boolean operationRunning() {
        return operationRunning;
    }

    public void setRepository(RepositoryInfo info, List<BranchInfo> branchList) {
        repositoryInfo = info;
        repository = info == null ? null : info.root();
        branches = branchList == null ? List.of() : List.copyOf(branchList);
    }

    public void setHistory(List<CommitEntry> entries) {
        history = entries == null ? List.of() : List.copyOf(entries);
    }

    public void setHistoryQuery(String query) {
        historyQuery = query == null ? "" : query.trim();
    }

    public void setOperationRunning(boolean running) {
        operationRunning = running;
    }

    public List<CommitEntry> filteredHistory() {
        String query = historyQuery.toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return history;
        }
        return history.stream()
                .filter(entry -> entry.subject().toLowerCase(Locale.ROOT).contains(query)
                        || entry.message().toLowerCase(Locale.ROOT).contains(query)
                        || entry.authorName().toLowerCase(Locale.ROOT).contains(query)
                        || entry.authorEmail().toLowerCase(Locale.ROOT).contains(query)
                        || entry.references().stream().anyMatch(reference ->
                        reference.name().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    public boolean canStage(com.example.gitguiwindowsapp.model.FileChange change) {
        return !operationRunning && change != null
                && change.stageState() != com.example.gitguiwindowsapp.model.StageState.STAGED;
    }

    public boolean canUnstage(com.example.gitguiwindowsapp.model.FileChange change) {
        return !operationRunning && change != null && change.isStaged()
                && change.stageState() != com.example.gitguiwindowsapp.model.StageState.UNTRACKED;
    }

    public boolean canDeleteBranch(BranchInfo branch) {
        return !operationRunning && repository != null && branch != null
                && !branch.current() && branch.merged();
    }
}
