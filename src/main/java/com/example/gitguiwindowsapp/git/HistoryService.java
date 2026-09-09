package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.CommitEntry;

import java.nio.file.Path;
import java.util.List;

/**
 * コミット履歴と参照情報の取得を担当します。
 */
public final class HistoryService {
    private final GitCommandSupport commands;
    private final GitCommitParser commitParser = new GitCommitParser();

    public HistoryService() {
        this(new GitCommandRunner());
    }

    public HistoryService(GitCommandExecutor runner) {
        commands = new GitCommandSupport(runner);
    }

    public List<CommitEntry> list(Path directory) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        GitCommandResult headResult = commands.run(root, List.of("rev-parse", "--verify", "HEAD"));
        String headId = headResult.succeeded() ? headResult.standardOutput().trim() : "";
        GitCommandResult branchResult = commands.run(root,
                List.of("symbolic-ref", "--short", "-q", "HEAD"));
        String headBranch = branchResult.succeeded() ? branchResult.standardOutput().trim() : "";
        GitCommandResult referenceResult = commands.execute(root, "Read commit references",
                List.of("for-each-ref", "--format=%(refname)\u001f%(objectname)",
                        "refs/heads", "refs/remotes", "refs/tags"));
        var references = commitParser.parseReferences(referenceResult.standardOutput(),
                headBranch, headId);
        GitCommandResult commitResult = commands.execute(root, "Read commit history",
                List.of("log", "--all", "--max-count=500", "--date=iso-strict",
                        "--format=%H\u001f%h\u001f%an\u001f%ae\u001f%aI\u001f%P\u001f%s\u001f%B\u001e"));
        return commitParser.parseCommits(commitResult.standardOutput(), references);
    }
}
