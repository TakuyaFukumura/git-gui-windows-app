package com.example.gitguiwindowsapp.model;

import java.time.OffsetDateTime;
import java.util.List;

public record CommitEntry(
        String id,
        String shortId,
        String subject,
        String authorName,
        String authorEmail,
        OffsetDateTime committedAt,
        List<String> parents,
        List<CommitReference> references,
        List<GraphSegment> graph) {

    public CommitEntry {
        parents = List.copyOf(parents);
        references = List.copyOf(references);
        graph = List.copyOf(graph);
    }
}
