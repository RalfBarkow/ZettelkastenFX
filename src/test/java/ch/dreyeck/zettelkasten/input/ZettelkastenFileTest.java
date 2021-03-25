package ch.dreyeck.zettelkasten.input;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import ch.dreyeck.zettelkasten.zip.ZipFilteredReader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ZettelkastenFileTest {

    @Test
    void getZknFile() {
        ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());
        ZipFilteredReader reader = new ZipFilteredReader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", ZETTELKASTEN_OBJECT_PROPERTY);
        ZETTELKASTEN_OBJECT_PROPERTY = reader.filteredUnmarshallZipFile(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        assertFalse(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel().isEmpty());
    }
}