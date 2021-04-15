package ch.dreyeck.zettelkasten.box;

import ch.dreyeck.zettelkasten.xml.Zettel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.web.HTMLEditor;

public class ZettelController {

    @FXML
    public TextArea textAreaZettelTitle;

    @FXML
    public HTMLEditor htmlEditorZettelContent;

    public void show(Zettel selectedItem) {
        textAreaZettelTitle.setText(selectedItem.getTitle());
        htmlEditorZettelContent.setHtmlText(selectedItem.getContent());
    }

}
