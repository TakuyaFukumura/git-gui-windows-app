package com.example.gitguiwindowsapp.model;

import java.util.List;

public record DiffDocument(
        List<DiffFile> files,
        String rawText,
        boolean binary,
        boolean tooLarge) {

    public DiffDocument {
        files = List.copyOf(files == null ? List.of() : files);
        rawText = rawText == null ? "" : rawText;
    }

    public boolean isEmpty() {
        return files.isEmpty() && rawText.isBlank();
    }
}
