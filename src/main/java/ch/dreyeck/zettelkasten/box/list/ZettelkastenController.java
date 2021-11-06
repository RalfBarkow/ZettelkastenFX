package ch.dreyeck.zettelkasten.box.list;

import cern.extjfx.fxml.FxmlView;
import ch.dreyeck.zettelkasten.box.ZettelController;
import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import ch.dreyeck.zettelkasten.zip.Reader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ZettelkastenController {

    private ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    public Button btnLoad;

    @FXML
    private ListView<Zettel> zettelListView;

    public static ListCell<Zettel> call() {
        return new ZettelListViewCell();
    }

    @FXML
    public void initialize() {
        zettelListView.setCellFactory(listView -> call());
    }

    @FXML
    void readZknFileXMLAndSetDataModelForListView() {
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", ZETTELKASTEN_OBJECT_PROPERTY);
        ZETTELKASTEN_OBJECT_PROPERTY = reader.filter(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        zettelListView.setItems(FXCollections.<Zettel>observableList(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel()));
    }

    @FXML
    public void handleMouseClick(MouseEvent arg0) {
        showStageZettel(zettelListView.getSelectionModel().getSelectedItem());
        // getSelectedItem
        // Note that the returned value is a snapshot in time - if you wish to observe the selection model for changes to the selected item, […]

    }

    private void showStageZettel(Zettel selectedItem) {
        System.out.println("clicked on " + selectedItem);

        // TODO getController(selectedModel)
        // all the dependencies (models, services, etc.)
        // are treated as singletons and kept in an internal cache

        // FIXME further mouse clicks should show the same Zettel

        Stage stageZettel = new Stage();
        FxmlView zettelView = new FxmlView(ZettelController.class);
        Scene scene = new Scene(zettelView.getRootNode());

        ZettelController zettelController = zettelView.getController();
        zettelController.show(selectedItem);

        stageZettel.setTitle("Zettel");
        stageZettel.setScene(scene);
        stageZettel.show();
    }

}