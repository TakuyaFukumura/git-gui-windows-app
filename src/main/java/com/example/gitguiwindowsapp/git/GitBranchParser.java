package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.BranchInfo;

import java.util.ArrayList;
import java.util.List;

public final class GitBranchParser {
    /**
     * Parses {@code git branch --format=%(refname:short)%09%(HEAD)} output.
     *
     * <p>Each non-empty record must contain exactly a branch name and an optional
     * {@code *} marker separated by a tab. Merged state is intentionally supplied
     * by {@link BranchService}, because it comes from a separate Git command.
     * Branch names and marker fields are preserved as machine-readable values;
     * UI labels are not generated here.</p>
     */
    public List<BranchInfo> parse(String output) {
        List<BranchInfo> branches = new ArrayList<>();
        if (output == null || output.isEmpty()) {
            return branches;
        }
        for (String line : output.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 2 || fields[0].isBlank()
                    || (!fields[1].isBlank() && !"*".equals(fields[1].trim()))) {
                throw new IllegalArgumentException("Invalid branch record.");
            }
            branches.add(new BranchInfo(fields[0].trim(), "*".equals(fields[1].trim())));
        }
        return List.copyOf(branches);
    }
}
