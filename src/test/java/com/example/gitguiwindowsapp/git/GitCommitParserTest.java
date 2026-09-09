package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.CommitReferenceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitCommitParserTest {
    private static final String TIME = "2026-09-09T12:34:56+09:00";

    @Test
    void parsesEmptyHistoryAndMergeParents() {
        GitCommitParser parser = new GitCommitParser();
        assertEquals(List.of(), parser.parseCommits("", Map.of()));

        String record = String.join("\u001f",
                "0123456789012345678901234567890123456789", "0123456",
                "Author", "author@example.com", TIME,
                "parent-one  parent-two", "Merge subject", "Full message") + "\u001e";

        var commit = parser.parseCommits(record, Map.of()).get(0);

        assertEquals(List.of("parent-one", "parent-two"), commit.parents());
        assertEquals("Merge subject", commit.subject());
    }

    @Test
    void rejectsMalformedCommitRecordsAndTimestamps() {
        GitCommitParser parser = new GitCommitParser();
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseCommits("invalid\u001e", Map.of()));

        String record = String.join("\u001f",
                "id", "short", "Author", "email", "not-a-time", "",
                "subject", "message") + "\u001e";
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseCommits(record, Map.of()));
    }

    @Test
    void classifiesReferencesWithoutAddingPresentationText() {
        String output = "refs/heads/main\u001fcommit-id\n"
                + "refs/remotes/origin/main\u001fcommit-id\n"
                + "refs/tags/v1\u001fcommit-id\n";

        var references = new GitCommitParser().parseReferences(output, "main", "commit-id");

        assertEquals(4, references.get("commit-id").size());
        assertEquals(CommitReferenceType.HEAD, references.get("commit-id").get(0).type());
    }
}
