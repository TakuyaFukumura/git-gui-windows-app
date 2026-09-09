package com.example.gitguiwindowsapp.ui;

import com.example.gitguiwindowsapp.model.DiffLine;
import com.example.gitguiwindowsapp.model.DiffLineType;
import com.example.gitguiwindowsapp.model.FileChange;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * 変更一覧と選択されたファイルの差分表示を担当します。
 */
public final class ChangesPane {
    private final TableView<FileChange> changesTable = new TableView<>();
    private final ListView<DiffLine> diffView = new ListView<>();
    private final SplitPane view;

    public ChangesPane(Consumer<FileChange> selectionChanged) {
        changesTable.setPlaceholder(new Label("変更ファイルはありません"));
        changesTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> selectionChanged.accept(newValue));
        configureTable();

        diffView.getStyleClass().add("diff-view");
        diffView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DiffLine line, boolean empty) {
                super.updateItem(line, empty);
                getStyleClass().removeAll("diff-addition", "diff-deletion", "diff-header",
                        "diff-hunk", "diff-meta", "diff-context");
                if (empty || line == null) {
                    setText(null);
                    return;
                }
                setText(line.text());
                getStyleClass().add(styleClassFor(line.type()));
            }
        });
        view = new SplitPane(changesTable, diffView);
        view.setDividerPositions(0.38);
    }

    private static String styleClassFor(DiffLineType type) {
        return switch (type) {
            case ADDITION -> "diff-addition";
            case DELETION -> "diff-deletion";
            case HEADER -> "diff-header";
            case HUNK -> "diff-hunk";
            case META -> "diff-meta";
            case CONTEXT -> "diff-context";
        };
    }

    private void configureTable() {
        TableColumn<FileChange, String> path = new TableColumn<>("ファイル");
        path.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().path()));
        path.setPrefWidth(420);
        TableColumn<FileChange, String> type = new TableColumn<>("変更");
        type.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().type().name()));
        TableColumn<FileChange, String> stage = new TableColumn<>("状態");
        stage.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().stageState().name()));
        changesTable.getColumns().add(path);
        changesTable.getColumns().add(type);
        changesTable.getColumns().add(stage);
    }

    public SplitPane view() {
        return view;
    }

    public FileChange selectedChange() {
        return changesTable.getSelectionModel().getSelectedItem();
    }

    public void setChanges(List<FileChange> changes) {
        changesTable.setItems(FXCollections.observableArrayList(changes));
    }

    public void setDiff(List<DiffLine> lines) {
        diffView.setItems(FXCollections.observableArrayList(lines));
    }

    public void clearDiff() {
        diffView.getItems().clear();
    }
}
