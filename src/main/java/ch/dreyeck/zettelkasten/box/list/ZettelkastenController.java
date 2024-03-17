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

import java.io.IOException;

public class ZettelkastenController {

    private ObjectProperty<Zettelkasten> zettelkastenObjectProperty = new SimpleObjectProperty<>(new Zettelkasten());
    private Reader reader;

    @FXML
    public Button btnLoad;

    @FXML
    private ListView<Zettel> zettelListView;

    private FxmlView zettelView = new FxmlView(ZettelController.class);
    private Scene scene = new Scene(zettelView.getRootNode());

    @FXML
    public void initialize() {
        zettelListView.setCellFactory(listView -> createZettelListViewCell());
    }

    private ListCell<Zettel> createZettelListViewCell() {
        return new ZettelListViewCell();
    }

    @FXML
    void readZknFileXMLAndSetDataModelForListView() {
        try {
            zettelkastenObjectProperty.set(getZettelkasten());
            zettelListView.setItems(FXCollections.observableList(zettelkastenObjectProperty.get().getZettel()));
        } catch (IOException e) {
            handleReadError(e);
        }
    }

    private Zettelkasten getZettelkasten() throws IOException {
        ensureReaderInitialized();
        ObjectProperty<Zettelkasten> zettelkastenProperty = reader.filter(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        if (zettelkastenProperty != null) {
            return zettelkastenProperty.get();
        } else {
            // Handle the case where zettelkastenObjectProperty is null
            // For example, display an error message or take appropriate action
            throw new IOException("Zettelkasten object property is null");
        }
    }

    private void ensureReaderInitialized() {
        if (reader == null) {
            reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", zettelkastenObjectProperty);
        }
    }

    @FXML
    private void handleMouseClick(MouseEvent event) {
        Zettel selectedItem = zettelListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            showZettel(selectedItem);
        }
    }

    private void showZettel(Zettel selectedItem) {
        System.out.println("Clicked on " + selectedItem);
        ZettelController zettelController = zettelView.getController();
        zettelController.show(selectedItem, scene);
    }

    private void handleReadError(Exception e) {
        e.printStackTrace(); // Handle the error appropriately, e.g., show an alert dialog
    }
}
