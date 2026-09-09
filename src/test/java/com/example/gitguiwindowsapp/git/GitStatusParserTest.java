package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.FileChangeType;
import com.example.gitguiwindowsapp.model.StageState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitStatusParserTest {
    @Test
    void parsesBranchAndNulSeparatedChanges() {
        String output = "## main...origin/main [ahead 2, behind 1]\0"
                + " M changed.txt\0"
                + "A  staged.txt\0"
                + "?? new file.txt\0"
                + "R  renamed.txt\0old.txt\0";

        GitStatusParser.ParsedStatus status = new GitStatusParser().parse(output);

        assertEquals("main", status.branch());
        assertEquals("origin/main", status.upstream());
        assertEquals(2, status.ahead());
        assertEquals(1, status.behind());
        assertEquals(4, status.changes().size());
        assertEquals(StageState.UNSTAGED, status.changes().get(0).stageState());
        assertEquals(FileChangeType.ADDED, status.changes().get(1).type());
        assertEquals(FileChangeType.UNTRACKED, status.changes().get(2).type());
        assertEquals("old.txt", status.changes().get(3).originalPath());
    }

    @Test
    void rejectsMalformedStatusRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> new GitStatusParser().parse("Mmissing"));
    }

    @Test
    void rejectsRenamesWithoutOriginalPath() {
        assertThrows(IllegalArgumentException.class,
                () -> new GitStatusParser().parse("R  renamed.txt\0"));
    }
}
