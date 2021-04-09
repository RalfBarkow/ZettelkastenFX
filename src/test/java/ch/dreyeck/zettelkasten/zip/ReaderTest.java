package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import nz.sodium.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ReaderTest {

    @Test
    void filter() {
        ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", ZETTELKASTEN_OBJECT_PROPERTY);
        ZETTELKASTEN_OBJECT_PROPERTY = getZknFileXML(reader);
        assertFalse(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel().isEmpty());
    }

    @Test
    void getContent() {
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3", new SimpleObjectProperty<>());
        String zettelContentCell = getContent(reader,
                1);
        assertFalse(zettelContentCell.isEmpty());
        System.out.println("zettelContentCell: " + zettelContentCell);
    }

    private String getContent(Reader reader, int z) {
        // z 0 throws IndexOutOfBoundsException: Index -1 out of bounds for length 4965
        int index = z - 1; // i=0 is Zettel #1
        return getZknFileXML(reader).getValue().getZettel().get(index).getContent();
    }

    private ObjectProperty<Zettelkasten> getZknFileXML(Reader reader) {
        return reader.filter(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
    }

    @Test
    void storeObjectPropertyToCell() {
        // xml.Zettelkasten is the Java class that represents a Zettelkasten.
        // Cell<Zettelkasten> represents a Zettelkasten that changes over time
        Zettelkasten zettelkasten = new Zettelkasten();
        Cell<Zettelkasten> zettelkastenCell;
    }
}