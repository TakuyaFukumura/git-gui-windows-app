package com.example.gitguiwindowsapp.model;

import java.util.List;

public record DiffFile(String oldPath, String newPath, List<DiffLine> lines) {
    public DiffFile {
        lines = List.copyOf(lines == null ? List.of() : lines);
    }

    public String path() {
        return newPath == null || "/dev/null".equals(newPath) ? oldPath : newPath;
    }
}
