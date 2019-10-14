package ch.dreyeck.zettelkastenfx;

import ch.dreyeck.zettelkasten.xml.Zettel;
import javafx.scene.control.ListCell;

class ZettelListViewCell extends ListCell<Zettel> {

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

