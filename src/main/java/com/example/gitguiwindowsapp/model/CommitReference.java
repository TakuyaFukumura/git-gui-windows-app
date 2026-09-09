package com.example.gitguiwindowsapp.model;

public record CommitReference(
        String name,
        CommitReferenceType type,
        boolean current) {
}
