package com.example.gitguiwindowsapp.model;

import java.util.Objects;

public record FileChange(
        String path,
        String originalPath,
        FileChangeType type,
        StageState stageState,
        char indexStatus,
        char workTreeStatus) {

    public FileChange {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(stageState, "stageState");
        originalPath = originalPath == null || originalPath.isBlank() ? null : originalPath;
    }

    public FileChange(String path, FileChangeType type, StageState stageState) {
        this(path, null, type, stageState, ' ', ' ');
    }

    public boolean isStaged() {
        return stageState == StageState.STAGED || stageState == StageState.BOTH;
    }

    public boolean isUnstaged() {
        return stageState == StageState.UNSTAGED
                || stageState == StageState.BOTH
                || stageState == StageState.UNTRACKED;
    }

    @Override
    public String toString() {
        return path;
    }
}
