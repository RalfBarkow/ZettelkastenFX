package ch.dreyeck.zettelkasten.input;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import ch.dreyeck.zettelkasten.zip.ZipFilteredReader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZettelkastenFileTest {

    @Test
    void getZknFile (){

        ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY = new SimpleObjectProperty<>(new Zettelkasten());

        File zknFileXML = new File("/Users/rgb/tmp/ziptest/zknFile.xml");
        zknFileXML.delete();

        /**
         * Here we create the ZipFilteredReader and configure it with a Predicate.
         * This predicate function is used to filter which files we want to copy
         * out of the zip file.
         */
        ZipFilteredReader reader = new ZipFilteredReader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/#303-DesktopFrame/rgb.zkn3", "/Users/rgb/tmp/ziptest");
        ZETTELKASTEN_OBJECT_PROPERTY = reader.filteredExpandZipFile(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        assertTrue(zknFileXML.exists());
        assertFalse(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel().isEmpty());

    }
}