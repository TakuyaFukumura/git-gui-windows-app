package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.FileChange;
import com.example.gitguiwindowsapp.model.FileChangeType;
import com.example.gitguiwindowsapp.model.StageState;

import java.util.ArrayList;
import java.util.List;

public final class GitStatusParser {
    private static int numberAfter(String text, int start) {
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        try {
            if (end == start) {
                throw new NumberFormatException("Missing tracking count.");
            }
            return Integer.parseInt(text.substring(start, end));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid branch tracking count.", e);
        }
    }

    private static StageState stageStateOf(char x, char y) {
        if (x == '?' && y == '?') {
            return StageState.UNTRACKED;
        }
        if (x == 'U' || y == 'U') {
            return StageState.UNMERGED;
        }
        boolean staged = x != ' ';
        boolean unstaged = y != ' ';
        if (staged && unstaged) {
            return StageState.BOTH;
        }
        return staged ? StageState.STAGED : unstaged ? StageState.UNSTAGED : StageState.CLEAN;
    }

    private static FileChangeType typeOf(char x, char y, String path) {
        if (x == '?' && y == '?') {
            return FileChangeType.UNTRACKED;
        }
        char status = x != ' ' ? x : y;
        return switch (status) {
            case 'A' -> FileChangeType.ADDED;
            case 'M' -> FileChangeType.MODIFIED;
            case 'D' -> FileChangeType.DELETED;
            case 'R' -> FileChangeType.RENAMED;
            case 'C' -> FileChangeType.COPIED;
            case 'U' -> FileChangeType.UNMERGED;
            default -> FileChangeType.UNKNOWN;
        };
    }

    /**
     * Parses {@code git status --porcelain=v1 -z --branch} output.
     *
     * <p>The first NUL-delimited record is the optional branch header. Change
     * records use the two-column porcelain v1 status followed by a path. Rename
     * and copy records consume one additional path record. Empty input means
     * that no status records were produced. Malformed records are rejected
     * rather than silently converted into a change.</p>
     */
    public ParsedStatus parse(String output) {
        if (output == null || output.isEmpty()) {
            return new ParsedStatus("", "", 0, 0, List.of());
        }
        String[] fields = output.split("\u0000", -1);
        String branch = "";
        String upstream = "";
        int ahead = 0;
        int behind = 0;
        List<FileChange> changes = new ArrayList<>();
        int index = 0;
        if (fields.length > 0 && fields[0].startsWith("##")) {
            String header = fields[0].substring(2).trim();
            String[] tracking = header.split("\\.\\.\\.", 2);
            String local = tracking[0];
            if (local.startsWith("No commits yet on ")) {
                local = local.substring("No commits yet on ".length());
            }
            int separator = local.indexOf(" (");
            branch = separator >= 0 ? local.substring(0, separator) : local;
            if (tracking.length == 2) {
                String remotePart = tracking[1];
                int space = remotePart.indexOf(' ');
                upstream = space >= 0 ? remotePart.substring(0, space) : remotePart;
            }
            int aheadMarker = header.indexOf("[ahead ");
            if (aheadMarker >= 0) {
                ahead = numberAfter(header, aheadMarker + 7);
            }
            int behindMarker = header.indexOf("behind ");
            if (behindMarker >= 0) {
                behind = numberAfter(header, behindMarker + 7);
            }
            index = 1;
        }
        while (index < fields.length) {
            String entry = fields[index++];
            if (entry.isEmpty() || entry.startsWith("##")) {
                continue;
            }
            if (entry.length() < 3 || entry.charAt(2) != ' ') {
                throw new IllegalArgumentException("Invalid status record.");
            }
            char x = entry.charAt(0);
            char y = entry.charAt(1);
            String path = entry.substring(3);
            String originalPath = null;
            FileChangeType type = typeOf(x, y, path);
            if (type == FileChangeType.RENAMED || type == FileChangeType.COPIED) {
                if (index >= fields.length || fields[index].isEmpty()) {
                    throw new IllegalArgumentException("Missing original path for rename or copy.");
                }
                originalPath = fields[index++];
            }
            changes.add(new FileChange(path, originalPath, type, stageStateOf(x, y), x, y));
        }
        return new ParsedStatus(branch, upstream, ahead, behind, changes);
    }

    public record ParsedStatus(String branch, String upstream, int ahead, int behind,
                               List<FileChange> changes) {
        public ParsedStatus {
            changes = List.copyOf(changes);
        }
    }
}
