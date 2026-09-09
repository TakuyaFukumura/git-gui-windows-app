package com.example.gitguiwindowsapp.application;

import com.example.gitguiwindowsapp.model.BranchInfo;
import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.CommitReference;
import com.example.gitguiwindowsapp.model.CommitReferenceType;
import com.example.gitguiwindowsapp.model.GraphSegment;
import com.example.gitguiwindowsapp.model.RepositoryInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AppStateTest {
    @Test
    void derivesRepositoryAndHistoryStateWithoutJavaFx() {
        AppState state = new AppState();
        RepositoryInfo info = new RepositoryInfo(Path.of("repo"), "main", "head", List.of(), true);
        CommitEntry commit = new CommitEntry("id", "short", "subject", "message",
                "author", "email", OffsetDateTime.now(), List.of(),
                List.of(new CommitReference("main", CommitReferenceType.LOCAL_BRANCH, true)),
                List.<GraphSegment>of());

        state.setRepository(info, List.of(new BranchInfo("main", true, true)));
        state.setHistory(List.of(commit));
        state.setHistoryQuery("AUTHOR");

        assertEquals(Path.of("repo"), state.repository());
        assertEquals(1, state.filteredHistory().size());
        assertFalse(state.canDeleteBranch(state.branches().get(0)));
        assertFalse(state.canStage(null));
    }
}
