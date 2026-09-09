package com.example.gitguiwindowsapp.ui;

import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.GraphSegment;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public final class CommitGraphView extends Region {
    private static final double LANE_WIDTH = 18;
    private static final double NODE_RADIUS = 4;
    private CommitEntry entry;

    public CommitGraphView(CommitEntry entry) {
        this.entry = entry;
        getStyleClass().add("commit-graph");
        setMinWidth(72);
        setPrefWidth(72);
        setMaxWidth(160);
    }

    private static double laneX(int lane) {
        return NODE_RADIUS + lane * LANE_WIDTH + 8;
    }

    public void setEntry(CommitEntry entry) {
        this.entry = entry;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        getChildren().clear();
        if (entry == null) {
            return;
        }
        double centerY = getHeight() / 2;
        for (GraphSegment segment : entry.graph()) {
            double fromX = laneX(segment.fromLane());
            double toX = laneX(segment.toLane());
            Line line = new Line(fromX, 0, toX, getHeight());
            line.getStyleClass().add(segment.highlighted() ? "graph-line-current" : "graph-line");
            getChildren().add(line);
        }
        int nodeLane = entry.graph().isEmpty() ? 0 : entry.graph().get(0).lane();
        Circle node = new Circle(laneX(nodeLane), centerY, NODE_RADIUS);
        node.getStyleClass().add("graph-node");
        getChildren().add(node);
    }
}
