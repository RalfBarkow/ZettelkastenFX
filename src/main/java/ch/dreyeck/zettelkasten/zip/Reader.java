package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.beans.property.ObjectProperty;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reader shows an example of filtering one or more matching
 * files from a ZipInputStream. Instead of unmarshalling the whole archive
 * this uses the Function interface to only unmarshall matching files/ZipEntries.
 */
public class Reader {
    private final Path zipLocation;
    private final ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY;

    /**
     * Constructs the filtered zip reader passing in the zip file to
     * be expanded by filter and the output ObjectProperty
     *
     * @param zipLocation                  the zip file
     * @param ZETTELKASTEN_OBJECT_PROPERTY for binding
     */
    public Reader(String zipLocation, ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY) {
        this.zipLocation = Paths.get(zipLocation);
        this.ZETTELKASTEN_OBJECT_PROPERTY = ZETTELKASTEN_OBJECT_PROPERTY;
    }

    /**
     * This method iterates through all entries in the zip archive. Each
     * entry is checked against the predicate (filter) that is passed to
     * the method. If the filter returns true, the entry is unmarshalled,
     * otherwise it is ignored.
     *
     * @param filter the predicate used to compare each entry against
     * @return ZETTELKASTEN_OBJECT_PROPERTY
     */
    public ObjectProperty<Zettelkasten> filter(Predicate<ZipEntry> filter)  {
        // we open the zip file using a java 7 try with resources block
        try (ZipInputStream stream = new ZipInputStream(new FileInputStream(zipLocation.toFile()))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (filter.test(entry)) {
                    unmarshallFromInputStream(stream);
                }
            }
        } catch (IOException e) { //getNextEntry
            e.printStackTrace();
        }
        return ZETTELKASTEN_OBJECT_PROPERTY;
    }

    /**
     * We only get here when the stream is located on a zip entry.
     * Now we can unmarshall from the stream for this current ZipEntry.
     */
    private void unmarshallFromInputStream(ZipInputStream stream) {
        try {
            final Unmarshaller unmarshaller =
                    JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
            ZETTELKASTEN_OBJECT_PROPERTY.set((Zettelkasten) unmarshaller.unmarshal(stream));
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }
}
