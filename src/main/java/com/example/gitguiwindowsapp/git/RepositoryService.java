package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.RepositoryInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * リポジトリの検証と状態取得を担当します。
 */
public final class RepositoryService {
    private final GitCommandSupport commands;
    private final GitStatusParser statusParser = new GitStatusParser();

    public RepositoryService() {
        this(new GitCommandRunner());
    }

    public RepositoryService(GitCommandRunner runner) {
        commands = new GitCommandSupport(runner);
    }

    public String gitVersion() throws GitCommandException {
        return commands.execute(Path.of("."), "git --version", List.of("--version"))
                .standardOutput().trim();
    }

    public Path validate(Path directory) throws GitCommandException {
        return commands.validateRepository(directory);
    }

    public RepositoryInfo status(Path directory) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        GitCommandResult result = commands.execute(root, "Read status",
                List.of("status", "--porcelain=v1", "-z", "--branch"));
        GitStatusParser.ParsedStatus parsed = statusParser.parse(result.standardOutput());
        GitCommandResult headResult = commands.run(root, List.of("rev-parse", "--verify", "HEAD"));
        String head = headResult.succeeded() ? headResult.standardOutput().trim() : "";
        return new RepositoryInfo(root, parsed.branch(), head, parsed.changes(),
                parsed.changes().isEmpty());
    }
}
