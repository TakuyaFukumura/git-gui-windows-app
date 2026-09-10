package com.example.gitguiwindowsapp.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.example.gitguiwindowsapp.model.CommitEntry;
import com.example.gitguiwindowsapp.model.CommitReference;
import com.example.gitguiwindowsapp.model.CommitReferenceType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 履歴検索、一覧、コミット詳細を担当します。
 */
public final class HistoryPane {
    private final ListView<CommitEntry> historyView = new ListView<>();
    private final TextField searchField = new TextField();
    private final Label id = new Label();
    private final Label author = new Label();
    private final Label date = new Label();
    private final Label parents = new Label();
    private final Label references = new Label();
    private final TextArea message = new TextArea();
    private final VBox view;
    private List<CommitEntry> entries = List.of();

    public HistoryPane(Consumer<String> queryChanged, Consumer<CommitEntry> selectionChanged) {
        searchField.setPromptText("メッセージ、作者、参照を検索");
        searchField.setTooltip(new Tooltip("コミットメッセージ、作者、参照名を検索"));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                queryChanged.accept(newValue));
        HBox toolbar = new HBox(8, new Label("履歴検索"), searchField);
        toolbar.getStyleClass().add("history-toolbar");

        historyView.getStyleClass().add("history-view");
        historyView.setPlaceholder(new Label("コミット履歴はありません"));
        historyView.setFixedCellSize(42);
        historyView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    showDetails(newValue);
                    selectionChanged.accept(newValue);
                });
        historyView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CommitEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                CommitGraphView graph = new CommitGraphView(entry);
                HBox referencesView = new HBox(4);
                referencesView.getStyleClass().add("commit-references");
                for (CommitReference reference : entry.references()) {
                    Label badge = new Label(reference.name());
                    badge.getStyleClass().add("commit-reference");
                    badge.getStyleClass().add(referenceClass(reference.type()));
                    badge.setTooltip(new Tooltip(reference.name()));
                    referencesView.getChildren().add(badge);
                }
                referencesView.setMaxWidth(260);
                Label subject = new Label(entry.subject());
                subject.getStyleClass().add("commit-subject");
                subject.setTextOverrun(OverrunStyle.ELLIPSIS);
                subject.setTooltip(new Tooltip(entry.subject()));
                HBox.setHgrow(subject, Priority.ALWAYS);
                Label metadata = new Label(entry.shortId() + "  " + entry.authorName() + "  "
                        + DateTimeFormatter.ISO_LOCAL_DATE.format(entry.committedAt()));
                metadata.getStyleClass().add("commit-metadata");
                setGraphic(new HBox(8, graph, referencesView, subject, metadata));
            }
        });

        VBox details = createDetails();
        SplitPane split = new SplitPane(historyView, details);
        split.setDividerPositions(0.7);
        VBox.setVgrow(split, Priority.ALWAYS);
        view = new VBox(8, toolbar, split);
    }

    private static VBox detailLine(String name, Label value) {
        return new VBox(2, new Label(name), value);
    }

    private static String referenceClass(CommitReferenceType type) {
        return switch (type) {
            case HEAD -> "commit-reference-head";
            case LOCAL_BRANCH -> "commit-reference-branch";
            case REMOTE_BRANCH -> "commit-reference-remote";
            case TAG -> "commit-reference-tag";
        };
    }

    private VBox createDetails() {
        Label title = new Label("コミット詳細");
        title.getStyleClass().add("commit-detail-title");
        message.setEditable(false);
        message.setWrapText(true);
        message.setPrefRowCount(8);
        message.setPromptText("コミットを選択してください");
        message.getStyleClass().add("commit-detail-message");
        for (Label label : List.of(id, author, date, parents, references)) {
            label.getStyleClass().add("commit-detail-value");
        }
        VBox details = new VBox(8, title, detailLine("ID", id), detailLine("作者", author),
                detailLine("日時", date), detailLine("親", parents), detailLine("参照", references), message);
        details.getStyleClass().add("commit-details");
        details.setPadding(new Insets(10));
        return details;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "The live JavaFX container must be returned for composition into the application scene.")
    public VBox view() {
        return view;
    }

    public CommitEntry selectedEntry() {
        return historyView.getSelectionModel().getSelectedItem();
    }

    public void setEntries(List<CommitEntry> entries) {
        this.entries = List.copyOf(entries);
        applyFilter(searchField.getText());
    }

    public void applyFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CommitEntry> filtered = entries.stream().filter(entry ->
                normalized.isBlank()
                        || entry.message().toLowerCase(Locale.ROOT).contains(normalized)
                        || entry.authorName().toLowerCase(Locale.ROOT).contains(normalized)
                        || entry.references().stream().map(CommitReference::name)
                        .anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(normalized))).toList();
        historyView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showDetails(CommitEntry entry) {
        boolean empty = entry == null;
        id.setText(empty ? "" : entry.id());
        author.setText(empty ? "" : entry.authorName() + " <" + entry.authorEmail() + ">");
        date.setText(empty ? "" : entry.committedAt().toString());
        parents.setText(empty ? "" : entry.parents().isEmpty() ? "なし" : String.join(", ", entry.parents()));
        references.setText(empty ? "" : entry.references().isEmpty()
                ? "なし" : entry.references().stream().map(CommitReference::name).toList().toString());
        message.setText(empty ? "" : entry.message());
    }
}
