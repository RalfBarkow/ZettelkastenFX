package ch.dreyeck.zettelkasten.fx.view;

import ch.dreyeck.zettelkasten.xml.Zettel;
import javafx.scene.control.ListCell;

public class ZettelListViewCell extends ListCell<Zettel> {

    @Override
    protected void updateItem(Zettel zettel, boolean empty) {
        super.updateItem(zettel, empty);
        if (empty || zettel == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(zettel.getTitle());
        }
    }

}

