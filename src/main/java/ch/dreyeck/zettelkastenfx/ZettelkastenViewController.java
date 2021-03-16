package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.io.File;

public class ZettelkastenViewController {

    private final ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    public Button btnLoad;

    @FXML
    private ListView<Zettel> zettelListView;

    public static ListCell<Zettel> call(ListView<Zettel> listView) {
        return new ZettelListViewCell();
    }

    @FXML
    public void initialize() {
        zettelListView.setCellFactory(ZettelkastenViewController::call);
    }

    @FXML
    void loadZknFileXML() {
        try {
            final Unmarshaller unmarshaller =
                    JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
            ZETTELKASTEN_OBJECT_PROPERTY.set((Zettelkasten) unmarshaller.unmarshal(new File("zknFile.xml")));
            zettelListView.setItems(FXCollections.<Zettel>observableList(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel()));
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}