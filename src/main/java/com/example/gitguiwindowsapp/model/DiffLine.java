package com.example.gitguiwindowsapp.model;

public record DiffLine(DiffLineType type, String text) {
    public DiffLine {
        text = text == null ? "" : text;
    }
}
