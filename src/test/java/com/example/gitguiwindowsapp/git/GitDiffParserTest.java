package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.DiffLineType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitDiffParserTest {
    @Test
    void parsesFilesAndLineKinds() {
        String diff = "diff --git a/file.txt b/file.txt\n"
                + "--- a/file.txt\n+++ b/file.txt\n"
                + "@@ -1 +1 @@\n-old\n+new\n";

        var document = new GitDiffParser().parse(diff);

        assertEquals(1, document.files().size());
        assertEquals("file.txt", document.files().get(0).path());
        assertTrue(document.files().get(0).lines().stream()
                .anyMatch(line -> line.type() == DiffLineType.ADDITION));
    }

    @Test
    void marksOversizedDiffAsNotDisplayable() {
        String large = "x".repeat(GitDiffParser.MAX_DIFF_BYTES + 1);
        var document = new GitDiffParser().parse(large);
        assertTrue(document.tooLarge());
        assertTrue(document.isEmpty());
    }

    @Test
    void preservesSpecialCharactersAndBinaryMarkers() {
        String diff = "diff --git \"a/name with space.txt\" \"b/name with space.txt\"\n"
                + "Binary files a/name with space.txt and b/name with space.txt differ\n";

        var document = new GitDiffParser().parse(diff);

        assertTrue(document.binary());
        assertEquals(diff, document.rawText());
    }
}
