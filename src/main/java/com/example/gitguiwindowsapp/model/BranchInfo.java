package com.example.gitguiwindowsapp.model;

import java.util.Objects;

public record BranchInfo(String name, boolean current) {
    public BranchInfo {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        return current ? name + " *" : name;
    }
}
