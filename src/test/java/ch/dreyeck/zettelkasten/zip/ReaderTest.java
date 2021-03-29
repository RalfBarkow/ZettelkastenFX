package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ReaderTest {

    @Test
    void filter() {
        ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", ZETTELKASTEN_OBJECT_PROPERTY);
        ZETTELKASTEN_OBJECT_PROPERTY = reader.filter(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        assertFalse(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel().isEmpty());
    }
}