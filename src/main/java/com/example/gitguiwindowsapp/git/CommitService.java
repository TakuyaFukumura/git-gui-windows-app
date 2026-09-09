package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.List;

/**
 * コミット実行とコミットID取得を担当します。
 */
public final class CommitService {
    private final GitCommandSupport commands;

    public CommitService() {
        this(new GitCommandRunner());
    }

    public CommitService(GitCommandExecutor runner) {
        commands = new GitCommandSupport(runner);
    }

    public GitOperationResult commit(Path directory, String message) throws GitCommandException {
        if (message == null || message.isBlank()) {
            throw new GitCommandException("Commit", -1, "Commit message must not be empty.");
        }
        Path root = commands.validateRepository(directory);
        GitOperationResult result = commands.operation(root, "Commit",
                List.of("commit", "-m", message.trim()));
        if (!result.success()) {
            return result;
        }
        GitCommandResult head = commands.run(root, List.of("rev-parse", "--verify", "HEAD"));
        return head.succeeded()
                ? new GitOperationResult(true, result.exitCode(), result.output(), result.error(),
                head.standardOutput().trim())
                : result;
    }
}
