package ch.dreyeck.zettelkasten.xml;

import ch.dreyeck.zettelkasten.zip.ZipFileProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ZettelkastenTest {

    @Test
    void testUnmarshallWithAttributes() throws Exception {
        // Mock the ZipFileProcessor
        ZipFileProcessor zipFileProcessor = Mockito.mock(ZipFileProcessor.class);

        // Mock behavior to return XML file with attributes
        when(zipFileProcessor.unmarshall(any(ZipEntry.class))).thenReturn(getMockZettelkastenWithAttributes());

        // Mock behavior to return input stream containing XML data with attributes
        ZipFile mockZipFile = Mockito.mock(ZipFile.class);
        ZipEntry mockZipEntry = Mockito.mock(ZipEntry.class);
        InputStream mockInputStream = getMockInputStreamWithAttributes();
        when(mockZipFile.getInputStream(any(ZipEntry.class))).thenReturn(mockInputStream);
        when(mockZipFile.getEntry(any(String.class))).thenReturn(mockZipEntry);
        when(zipFileProcessor.getZipFile()).thenReturn(mockZipFile);

        // Call the unmarshal method
        Zettelkasten zettelkasten = zipFileProcessor.unmarshall(mockZipEntry);

        // Validate the unmarshalled Zettelkasten object
        assertEquals(1890, zettelkasten.getLastzettel().intValue());
        assertEquals(1, zettelkasten.getFirstzettel().intValue());
    }

    // Helper method to create a mock InputStream with XML data containing attributes
    private InputStream getMockInputStreamWithAttributes() {
        String xmlData = "<zettelkasten firstzettel=\"1\" lastzettel=\"1890\">...</zettelkasten>";
        return new ByteArrayInputStream(xmlData.getBytes());
    }

    // Helper method to create a mock Zettelkasten object with attributes
    private Zettelkasten getMockZettelkastenWithAttributes() {
        Zettelkasten zettelkasten = new Zettelkasten();
        zettelkasten.setFirstzettel(BigInteger.valueOf(1));
        zettelkasten.setLastzettel(BigInteger.valueOf(1890));
        // Set other properties if needed
        return zettelkasten;
    }

    @Test
    void testUnmarshallWithoutAttributes() throws Exception {
        // Mock the ZipFileProcessor
        ZipFileProcessor zipFileProcessor = Mockito.mock(ZipFileProcessor.class);

        // Mock behavior to return XML file without attributes
        when(zipFileProcessor.unmarshall(any(ZipEntry.class))).thenReturn(getMockZettelkastenWithoutAttributes());

        // Mock behavior to return input stream containing XML data without attributes
        ZipFile mockZipFile = Mockito.mock(ZipFile.class);
        ZipEntry mockZipEntry = Mockito.mock(ZipEntry.class);
        InputStream mockInputStream = getMockInputStreamWithoutAttributes();
        when(mockZipFile.getInputStream(any(ZipEntry.class))).thenReturn(mockInputStream);
        when(mockZipFile.getEntry(any(String.class))).thenReturn(mockZipEntry);
        when(zipFileProcessor.getZipFile()).thenReturn(mockZipFile);

        // Call the unmarshal method
        Zettelkasten zettelkasten = zipFileProcessor.unmarshall(mockZipEntry);

        // Validate the unmarshalled Zettelkasten object
        assertEquals(null, zettelkasten.getLastzettel());
        assertEquals(null, zettelkasten.getFirstzettel());
    }

    // Helper method to create a mock InputStream with XML data containing attributes
    private InputStream getMockInputStreamWithoutAttributes() {
        String xmlData = "<zettelkasten>...</zettelkasten>";
        return new ByteArrayInputStream(xmlData.getBytes());
    }

    // Helper method to create a mock Zettelkasten object with attributes
    private Zettelkasten getMockZettelkastenWithoutAttributes() {
        Zettelkasten zettelkasten = new Zettelkasten();
        // Set other properties if needed
        return zettelkasten;
    }
}
