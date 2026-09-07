package com.example.gitguiwindowsapp.git;

public record GitCommandResult(
        int exitCode,
        String standardOutput,
        String standardError,
        long durationMillis) {

    public boolean succeeded() {
        return exitCode == 0;
    }
}
