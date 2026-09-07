package com.example.gitguiwindowsapp.model;

public record GitOperationResult(
        boolean success,
        int exitCode,
        String output,
        String error,
        String commitId) {

    public GitOperationResult {
        output = output == null ? "" : output;
        error = error == null ? "" : error;
        commitId = commitId == null ? "" : commitId;
    }

    public GitOperationResult(boolean success, int exitCode, String output, String error) {
        this(success, exitCode, output, error, "");
    }
}
