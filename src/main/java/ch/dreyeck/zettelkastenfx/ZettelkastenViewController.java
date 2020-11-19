package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class ZettelkastenViewController {

    private final ObjectProperty<Zettelkasten> zettelkasten = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    private ListView<Zettel> zettelListView;

    private static ListCell<Zettel> call(ListView<Zettel> listView) {
        return new ZettelListViewCell();
    }

    @FXML
    public void initialize() {
        zettelListView.setCellFactory(ZettelkastenViewController::call);
    }

    @FXML
    void loadZettelkasten(final ActionEvent event) {
        try {
            final Unmarshaller unmarshaller =
                    JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
            zettelkasten.set((Zettelkasten) unmarshaller.unmarshal(new File("zknFile.xml")));
            zettelListView.setItems(FXCollections.<Zettel>observableList(zettelkasten.getValue().getZettel()));
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}