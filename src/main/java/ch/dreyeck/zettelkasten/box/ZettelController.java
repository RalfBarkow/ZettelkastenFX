package ch.dreyeck.zettelkasten.box;

import ch.dreyeck.zettelkasten.xml.Zettel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ZettelController {

    @FXML
    public TextArea textAreaZettel;

    public void showContent(Zettel selectedItem) {
        textAreaZettel.setText(selectedItem.getContent());
    }

}
