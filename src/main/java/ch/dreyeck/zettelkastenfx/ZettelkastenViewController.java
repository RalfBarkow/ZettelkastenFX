package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ZettelkastenViewController implements Initializable {

    private final ObjectProperty<Zettelkasten> zettelkasten = new SimpleObjectProperty<>(new Zettelkasten());

    @FXML
    private ListView<String> listView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        ObservableList<String> items = listView.getItems(); // erst getItems !!!
        items.add("One");
        items.add("Two");
        items.add("Three");
        items.add("Four");
        items.add("Five");
    }

    @FXML
    void loadZettelkasten(final ActionEvent event) {
        try {
            final Unmarshaller unmarshaller =
                    JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
            zettelkasten.set((Zettelkasten) unmarshaller.unmarshal(new File("zknFile.xml")));

        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}