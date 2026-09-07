package com.example.gitguiwindowsapp.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record RepositoryInfo(
        Path root,
        String branch,
        String head,
        List<FileChange> changes,
        boolean clean) {

    public RepositoryInfo {
        Objects.requireNonNull(root, "root");
        changes = List.copyOf(changes == null ? List.of() : changes);
        branch = branch == null || branch.isBlank() ? "(detached HEAD)" : branch;
        head = head == null ? "" : head;
    }
}
