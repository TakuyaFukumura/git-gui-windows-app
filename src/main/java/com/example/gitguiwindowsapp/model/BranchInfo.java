package com.example.gitguiwindowsapp.model;

import java.util.Objects;

public record BranchInfo(String name, boolean current, boolean merged) {
    public BranchInfo(String name, boolean current) {
        this(name, current, false);
    }

    public BranchInfo {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        return current ? name + " *" : name;
    }
}
