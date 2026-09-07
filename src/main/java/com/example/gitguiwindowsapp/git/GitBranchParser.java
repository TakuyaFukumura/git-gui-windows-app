package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.BranchInfo;

import java.util.ArrayList;
import java.util.List;

public final class GitBranchParser {
    public List<BranchInfo> parse(String output) {
        List<BranchInfo> branches = new ArrayList<>();
        if (output == null) {
            return branches;
        }
        for (String line : output.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", 2);
            branches.add(new BranchInfo(fields[0].trim(),
                    fields.length > 1 && "*".equals(fields[1].trim()),
                    fields.length > 2 && "merged".equals(fields[2].trim())));
        }
        return List.copyOf(branches);
    }
}
