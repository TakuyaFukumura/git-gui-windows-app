package com.example.gitguiwindowsapp.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelContractTest {
    @Test
    void rejectsNullRequiredCommitValuesAndKeepsCollectionsImmutable() {
        assertThrows(NullPointerException.class,
                () -> new CommitReference(null, CommitReferenceType.HEAD, false));
        assertThrows(NullPointerException.class,
                () -> new CommitEntry(null, "short", "subject", "message", "name",
                        "email", OffsetDateTime.now(), List.of(), List.of(), List.of()));

        var entry = new CommitEntry("id", "short", "subject", "message", "name",
                "email", OffsetDateTime.now(), null, null, null);

        assertEquals(List.of(), entry.parents());
        assertThrows(UnsupportedOperationException.class,
                () -> entry.parents().add("parent"));
    }
}
