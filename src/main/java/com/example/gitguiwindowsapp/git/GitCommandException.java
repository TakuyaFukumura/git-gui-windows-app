package com.example.gitguiwindowsapp.git;

public class GitCommandException extends Exception {
    private final String operation;
    private final int exitCode;
    private final String standardError;

    public GitCommandException(String operation, int exitCode, String standardError) {
        super(formatMessage(operation, exitCode, standardError));
        this.operation = operation;
        this.exitCode = exitCode;
        this.standardError = standardError == null ? "" : standardError;
    }

    public GitCommandException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.exitCode = -1;
        this.standardError = message;
    }

    private static String formatMessage(String operation, int exitCode, String error) {
        String detail = error == null || error.isBlank() ? "Git command failed." : error.trim();
        return operation + " failed (exit " + exitCode + "): " + detail;
    }

    public String operation() {
        return operation;
    }

    public int exitCode() {
        return exitCode;
    }

    public String standardError() {
        return standardError;
    }
}
