package com.example.gitguiwindowsapp.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitBranchParserTest {
    @Test
    void parsesTheExplicitBranchFormat() {
        var branches = new GitBranchParser().parse("main\t*\nfeature\t\n");

        assertEquals(2, branches.size());
        assertTrue(branches.get(0).current());
        assertEquals("feature", branches.get(1).name());
    }

    @Test
    void rejectsUnexpectedFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new GitBranchParser().parse("main\t*\textra\n"));
    }
}
