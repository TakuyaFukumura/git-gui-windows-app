package com.example.gitguiwindowsapp.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Git実行の差し替え境界。サービスはProcessBuilderへ直接依存しません。
 */
@FunctionalInterface
public interface GitCommandExecutor {
    GitCommandResult run(Path workingDirectory, List<String> arguments)
            throws IOException, InterruptedException;
}
