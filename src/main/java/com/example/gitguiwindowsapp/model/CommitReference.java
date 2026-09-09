package com.example.gitguiwindowsapp.model;

import java.util.Objects;

public record CommitReference(
        String name,
        CommitReferenceType type,
        boolean current) {
    public CommitReference {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
