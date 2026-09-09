package com.example.gitguiwindowsapp.model;

public record GraphSegment(
        int lane,
        int fromLane,
        int toLane,
        GraphSegmentKind kind,
        boolean highlighted) {
}
