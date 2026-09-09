package com.example.gitguiwindowsapp.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record CommitEntry(
        String id,
        String shortId,
        String subject,
        String message,
        String authorName,
        String authorEmail,
        OffsetDateTime committedAt,
        List<String> parents,
        List<CommitReference> references,
        List<GraphSegment> graph) {

    public CommitEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(shortId, "shortId");
            Objects.requireNonNull(subject, "subject");
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(authorName, "authorName");
            Objects.requireNonNull(authorEmail, "authorEmail");
            Objects.requireNonNull(committedAt, "committedAt");
            parents = List.copyOf(parents == null ? List.of() : parents);
            references = List.copyOf(references == null ? List.of() : references);
            graph = List.copyOf(graph == null ? List.of() : graph);
        }
}
