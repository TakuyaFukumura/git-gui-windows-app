package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.DiffFile;
import com.example.gitguiwindowsapp.model.DiffLine;
import com.example.gitguiwindowsapp.model.DiffLineType;
import com.example.gitguiwindowsapp.model.DiffDocument;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GitDiffParser {
    public static final int MAX_DIFF_BYTES = 10 * 1024 * 1024;

    /**
     * Parses Git's unified diff output into immutable display-neutral models.
     *
     * <p>Empty input produces an empty document. Binary markers are retained
     * as metadata, while oversized input is not parsed and is marked
     * {@code tooLarge}. The original UTF-8 text is retained so the UI can
     * report Git metadata without putting presentation strings in this parser.</p>
     */
    public DiffDocument parse(String diff) {
        String source = diff == null ? "" : diff;
        boolean tooLarge = source.getBytes(StandardCharsets.UTF_8).length > MAX_DIFF_BYTES;
        if (tooLarge) {
            return new DiffDocument(List.of(), "", false, true);
        }
        if (source.isBlank()) {
            return new DiffDocument(List.of(), source, false, false);
        }
        List<DiffFile> files = new ArrayList<>();
        List<DiffLine> lines = new ArrayList<>();
        String oldPath = null;
        String newPath = null;
        boolean binary = false;
        for (String line : source.split("\\R", -1)) {
            if (line.startsWith("diff --git ")) {
                if (oldPath != null || !lines.isEmpty()) {
                    files.add(new DiffFile(oldPath, newPath, lines));
                    lines = new ArrayList<>();
                }
                oldPath = null;
                newPath = null;
                lines.add(new DiffLine(DiffLineType.HEADER, line));
            } else if (line.startsWith("--- ")) {
                oldPath = stripPrefix(line.substring(4));
                lines.add(new DiffLine(DiffLineType.HEADER, line));
            } else if (line.startsWith("+++ ")) {
                newPath = stripPrefix(line.substring(4));
                lines.add(new DiffLine(DiffLineType.HEADER, line));
            } else if (line.startsWith("@@")) {
                lines.add(new DiffLine(DiffLineType.HUNK, line));
            } else if (line.startsWith("Binary files ") || line.startsWith("GIT binary patch")) {
                binary = true;
                lines.add(new DiffLine(DiffLineType.META, line));
            } else if (line.startsWith("+")) {
                lines.add(new DiffLine(DiffLineType.ADDITION, line));
            } else if (line.startsWith("-")) {
                lines.add(new DiffLine(DiffLineType.DELETION, line));
            } else {
                lines.add(new DiffLine(DiffLineType.CONTEXT, line));
            }
        }
        if (oldPath != null || newPath != null || !lines.isEmpty()) {
            files.add(new DiffFile(oldPath, newPath, lines));
        }
        return new DiffDocument(files, source, binary, false);
    }

    private static String stripPrefix(String path) {
        if ("/dev/null".equals(path)) {
            return path;
        }
        if (path.length() > 2 && (path.startsWith("a/") || path.startsWith("b/"))) {
            return path.substring(2);
        }
        return path;
    }
}
