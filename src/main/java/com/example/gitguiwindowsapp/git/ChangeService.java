package com.example.gitguiwindowsapp.git;

import com.example.gitguiwindowsapp.model.DiffDocument;
import com.example.gitguiwindowsapp.model.GitOperationResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 変更一覧の差分表示とステージ操作を担当します。
 */
public final class ChangeService {
    private final GitCommandSupport commands;
    private final GitDiffParser diffParser = new GitDiffParser();

    public ChangeService() {
        this(new GitCommandRunner());
    }

    public ChangeService(GitCommandExecutor runner) {
        commands = new GitCommandSupport(runner);
    }

    public DiffDocument diff(Path directory, String path, boolean cached) throws GitCommandException {
        Path root = commands.validateRepository(directory);
        List<String> arguments = new ArrayList<>(List.of("diff", "--no-ext-diff", "--unified=3"));
        if (cached) {
            arguments.add("--cached");
        }
        if (path != null && !path.isBlank()) {
            arguments.add("--");
            arguments.add(path);
        }
        return diffParser.parse(commands.execute(root, "Read diff", arguments).standardOutput());
    }

    public GitOperationResult stage(Path directory, String path) throws GitCommandException {
        return commands.operation(directory, "Stage file",
                List.of("add", "--", GitCommandSupport.requirePath(path)));
    }

    public GitOperationResult unstage(Path directory, String path) throws GitCommandException {
        return commands.operation(directory, "Unstage file",
                List.of("reset", "--", GitCommandSupport.requirePath(path)));
    }
}
