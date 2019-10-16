package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

public class ZettelkastenViewController {

    private final ObjectProperty<Zettelkasten> zettelkasten = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    private ListView<Zettel> listView;

    @FXML
    public void initialize() {
        listView.setCellFactory(listView -> new ZettelListViewCell());
    }

    @FXML
    void loadZettelkasten(final ActionEvent event) {
        try {
            final Unmarshaller unmarshaller =
                    JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
            zettelkasten.set((Zettelkasten) unmarshaller.unmarshal(new File("zknFile.xml")));
            listView.setItems(FXCollections.<Zettel>observableList(zettelkasten.getValue().getZettel()));
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}