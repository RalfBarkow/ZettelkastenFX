package ch.dreyeck.zettelkasten.input;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZettelkastenTest {

    @Test
    void getZknFile (){

        File zknFileXML = new File("/Users/rgb/tmp/ziptest/zknFile.xml");
        zknFileXML.delete();

        /**
         * Here we create the ZipFilteredReader and configure it with a Predicate.
         * This predicate function is used to filter which files we want to copy
         * out of the zip file.
         */
        ZipFilteredReader reader = new ZipFilteredReader("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/#303-DesktopFrame/rgb.zkn3", "/Users/rgb/tmp/ziptest");
        reader.filteredExpandZipFile(zipEntry -> zipEntry.getName().equals("zknFile.xml"));
        assertTrue(zknFileXML.exists());

    }
}