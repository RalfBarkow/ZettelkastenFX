package ch.dreyeck.zettelkasten.list;

import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import ch.dreyeck.zettelkasten.zip.Reader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.ResourceBundle;

public class ZettelkastenController implements Initializable {

    private ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    public Button btnLoad;

    @FXML
    private ListView<Zettel> zettelListView;

    public static ListCell<Zettel> call() {
        return new ZettelListViewCell();
    }

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        zettelListView.setCellFactory(listView -> call());
    }

    @FXML
    void getZknFileXML() {
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", ZETTELKASTEN_OBJECT_PROPERTY);
        ZETTELKASTEN_OBJECT_PROPERTY = reader.filter(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        zettelListView.setItems(FXCollections.<Zettel>observableList(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel()));
    }
}