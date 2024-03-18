package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class ReaderTest {

    @Test
    void testFilterWithMatchingEntries() {
        // Prepare test data
        ObjectProperty<Zettelkasten> zettelkastenProperty = new SimpleObjectProperty<>(new Zettelkasten());
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/firstzettel.zkn3", zettelkastenProperty);

        // Define filter predicate to match specific entries
        Predicate<ZipEntry> filter = entry -> entry.getName().equals("zknFile.xml");

        // Call the method under test
        ObjectProperty<Zettelkasten> filteredZettelkastenProperty = reader.filter(filter);

        // Verify that the filtered entry is not null
        assertNotNull(filteredZettelkastenProperty, "Filtered Zettelkasten object property should not be null");
    }

    @Test
    void testFilterWithNoMatchingEntries() {
        // Prepare test data
        ObjectProperty<Zettelkasten> zettelkastenProperty = new SimpleObjectProperty<>(new Zettelkasten());
        Reader reader = new Reader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/firstzettel.zkn3", zettelkastenProperty);

        // Define filter predicate to match no entries
        Predicate<ZipEntry> filter = entry -> false; // No entries will match

        // Call the method under test
        ObjectProperty<Zettelkasten> filteredZettelkastenProperty = reader.filter(filter);

        // Verify that the filtered entry is an ObjectProperty with a null value
        assertEquals(null, filteredZettelkastenProperty.getValue(), "Filtered Zettelkasten object property should be null");
    }
}
