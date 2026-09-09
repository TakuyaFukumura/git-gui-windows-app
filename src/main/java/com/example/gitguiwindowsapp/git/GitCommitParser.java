package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.CommitReference;
import com.example.gitguiwindowsapp.model.CommitReferenceType;
import com.example.gitguiwindowsapp.model.GraphSegment;
import com.example.gitguiwindowsapp.model.GraphSegmentKind;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GitCommitParser {
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final String RECORD_SEPARATOR = "\u001e";

    /**
     * Parses records emitted by {@code git log --format} using control
     * characters as delimiters. Eight fields are required. Empty output is a
     * valid empty history; malformed records or timestamps fail explicitly.
     * Parent IDs are separated by arbitrary whitespace, as produced by Git for
     * both ordinary and merge commits.
     */
    List<CommitEntry> parseCommits(String output, Map<String, List<CommitReference>> references) {
        if (output == null || output.isEmpty()) {
            return List.of();
        }
        List<RawCommit> rawCommits = new ArrayList<>();
        for (String record : output.split(RECORD_SEPARATOR, -1)) {
            if (record.isBlank()) {
                continue;
            }
            String[] fields = record.split(FIELD_SEPARATOR, -1);
            if (fields.length != 8 || fields[0].isBlank() || fields[4].isBlank()) {
                throw new IllegalArgumentException("Invalid commit history record.");
            }
            OffsetDateTime committedAt;
            try {
                committedAt = OffsetDateTime.parse(fields[4]);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid commit timestamp.", e);
            }
            rawCommits.add(new RawCommit(fields[0], fields[1], fields[2], fields[3],
                    committedAt, fields[5].isBlank() ? List.of() : List.of(fields[5].trim().split("\\s+")),
                    fields[6], fields[7]));
        }
        return addGraph(rawCommits, references);
    }

    Map<String, List<CommitReference>> parseReferences(String output, String headBranch,
                                                        String headId) {
        Map<String, List<CommitReference>> references = new HashMap<>();
        if (!headId.isBlank()) {
            addReference(references, headId, new CommitReference("HEAD",
                    CommitReferenceType.HEAD, true));
        }
        if (output == null || output.isEmpty()) {
            return references;
        }
        for (String line : output.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(FIELD_SEPARATOR, -1);
            if (fields.length != 2 || fields[0].isBlank() || fields[1].isBlank()) {
                throw new IllegalArgumentException("Invalid reference record.");
            }
            String refName = fields[0];
            CommitReferenceType type;
            String displayName;
            if (refName.startsWith("refs/heads/")) {
                type = CommitReferenceType.LOCAL_BRANCH;
                displayName = refName.substring("refs/heads/".length());
            } else if (refName.startsWith("refs/remotes/")) {
                type = CommitReferenceType.REMOTE_BRANCH;
                displayName = refName.substring("refs/remotes/".length());
            } else if (refName.startsWith("refs/tags/")) {
                type = CommitReferenceType.TAG;
                displayName = refName.substring("refs/tags/".length());
            } else {
                continue;
            }
            addReference(references, fields[1], new CommitReference(displayName, type,
                    type == CommitReferenceType.LOCAL_BRANCH && displayName.equals(headBranch)));
        }
        return references;
    }

    private List<CommitEntry> addGraph(List<RawCommit> commits,
                                       Map<String, List<CommitReference>> references) {
        List<String> lanes = new ArrayList<>();
        List<CommitEntry> entries = new ArrayList<>();
        for (RawCommit commit : commits) {
            int lane = lanes.indexOf(commit.id());
            if (lane < 0) {
                lane = 0;
                lanes.add(commit.id());
            }
            List<GraphSegment> graph = graphFor(commit, lanes, lane);
            entries.add(new CommitEntry(commit.id(), commit.shortId(), commit.subject(),
                    commit.message(), commit.authorName(), commit.authorEmail(), commit.committedAt(),
                    commit.parents(), references.getOrDefault(commit.id(), List.of()), graph));
            lanes.remove(lane);
            for (int index = commit.parents().size() - 1; index >= 0; index--) {
                String parent = commit.parents().get(index);
                if (!lanes.contains(parent)) {
                    lanes.add(lane, parent);
                }
            }
        }
        return entries;
    }

    private List<GraphSegment> graphFor(RawCommit commit, List<String> lanes, int lane) {
        List<GraphSegment> graph = new ArrayList<>();
        graph.add(new GraphSegment(lane, lane, lane, GraphSegmentKind.CONTINUATION, true));
        for (int index = 0; index < commit.parents().size(); index++) {
            String parent = commit.parents().get(index);
            int targetLane = lanes.indexOf(parent);
            if (targetLane < 0) {
                targetLane = lane + index;
            }
            GraphSegmentKind kind = commit.parents().size() > 1
                    ? GraphSegmentKind.MERGE
                    : targetLane == lane ? GraphSegmentKind.CONTINUATION : GraphSegmentKind.BRANCH;
            graph.add(new GraphSegment(lane, lane, targetLane, kind, true));
        }
        return graph;
    }

    private static void addReference(Map<String, List<CommitReference>> references,
                                     String commitId, CommitReference reference) {
        references.computeIfAbsent(commitId, ignored -> new ArrayList<>()).add(reference);
    }

    private record RawCommit(
            String id,
            String shortId,
            String authorName,
            String authorEmail,
            OffsetDateTime committedAt,
            List<String> parents,
            String subject,
            String message) {
    }
}
