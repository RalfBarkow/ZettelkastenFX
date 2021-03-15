package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.input.ZipFilteredReader;
import ch.dreyeck.zettelkasten.xml.Zettel;
import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class ZettelkastenViewController {

    private final ObjectProperty<Zettelkasten> zettelkasten = new SimpleObjectProperty<>(new Zettelkasten());
    public Button btnLoad;

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
            zettelkasten.set((Zettelkasten) unmarshaller.unmarshal(new ZipFilteredReader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/#303-DesktopFrame/rgb.zkn3", "/Users/rgb/tmp/ziptest").filteredExpandZipFile(zipEntry -> zipEntry.getName().equals("zknFile.xml"))));
            zettelListView.setItems(FXCollections.<Zettel>observableList(zettelkasten.getValue().getZettel()));
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}