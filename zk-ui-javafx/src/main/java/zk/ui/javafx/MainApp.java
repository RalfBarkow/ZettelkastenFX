package zk.ui.javafx;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import zk.core.model.NoteDTO;
import zk.core.model.NoteId;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainApp extends Application {

  private SQLiteRuntime rt;
  private final ObservableList<NoteDTO> noteItems = FXCollections.observableArrayList();

  @Override public void start(Stage stage) {
    // storage wiring
    this.rt = new SQLiteRuntime();

    // UI controls
    ListView<NoteDTO> list = new ListView<>(noteItems);
    list.setCellFactory(lv -> new ListCell<NoteDTO>() {
      @Override protected void updateItem(NoteDTO n, boolean empty) {
        super.updateItem(n, empty);
        setText(empty || n == null ? "" : (n.id().value() + " · " + n.title()));
      }
    });

    TextField title = new TextField();
    TextArea body = new TextArea();
    body.setWrapText(true);

    // toolbar
    Button newBtn = new Button("New Note");
    TextField search = new TextField();
    search.setPromptText("Search (title/body, naive)");
    ToolBar toolbar = new ToolBar(newBtn, new Separator(), search);

    // layout
    VBox editor = new VBox(6, title, body);
    VBox.setVgrow(body, Priority.ALWAYS);
    BorderPane root = new BorderPane();
    root.setTop(toolbar);
    root.setLeft(list);
    root.setCenter(editor);

    // load data
    refreshList();
    if (!noteItems.isEmpty()) list.getSelectionModel().select(0);

    // selection binding (read → fields)
    ChangeListener<NoteDTO> selListener = (obs, oldN, newN) -> {
      if (newN == null) { title.setText(""); body.setText(""); return; }
      title.setText(newN.title());
      body.setText(newN.body());
    };
    list.getSelectionModel().selectedItemProperty().addListener(selListener);

    // edits (fields → DB)
    title.setOnAction(e -> persistEdit(list, title, body));
    body.setOnKeyPressed(ev -> {
      if (ev.isMetaDown() && ev.getCode() == KeyCode.ENTER) { // ⌘ Enter to save
        persistEdit(list, title, body);
      }
    });

    // new note
    newBtn.setOnAction(e -> {
      NoteId id = rt.notes.create("New note", "");
      refreshList();
      selectById(list, id);
      title.requestFocus();
      title.selectAll();
    });

    // very naive search over loaded list (we'll add FTS later)
    search.setOnAction(e -> {
      String q = search.getText().toLowerCase();
      NoteDTO hit = noteItems.stream()
              .filter(n -> n.title().toLowerCase().contains(q) || n.body().toLowerCase().contains(q))
              .findFirst().orElse(null);
      if (hit != null) selectById(list, hit.id());
    });

    // show window
    Scene scene = new Scene(root, 1100, 680);
    stage.setTitle("ZettelkastenFX (SQLite)");
    stage.setScene(scene);
    stage.show();

    // seed one note if DB was empty
    if (noteItems.isEmpty()) {
      NoteId id = rt.notes.create(
              "Welcome to ZettelkastenFX",
              "Type here. Press ⌘ Enter to save.\nThis is a real SQLite-backed note.");
      refreshList();
      selectById(list, id);
    }
  }

  private void persistEdit(ListView<NoteDTO> list, TextField title, TextArea body) {
    NoteDTO cur = list.getSelectionModel().getSelectedItem();
    if (cur == null) return;
    NoteDTO upd = new NoteDTO(cur.id(), title.getText(), body.getText(),
            cur.createdAt(), Instant.now(), cur.rating());
    rt.notes.update(upd);
    refreshList();
    selectById(list, cur.id());
  }

  private void refreshList() {
    List<NoteDTO> all = rt.notes.all()
            .sorted(Comparator.comparingInt(n -> n.id().value()))
            .collect(Collectors.toList());
    noteItems.setAll(all);
  }

  private void selectById(ListView<NoteDTO> list, NoteId id) {
    for (int i = 0; i < noteItems.size(); i++) {
      if (noteItems.get(i).id().equals(id)) { list.getSelectionModel().select(i); break; }
    }
  }

  public static void main(String[] args){ launch(args); }
}
