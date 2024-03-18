package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ZipFileProcessorTest {

    private ZipFileProcessor zipFileProcessor;
    private ZipFile mockZipFile;
    private InputStream mockInputStream;

    @BeforeEach
    void setUp() throws IOException {
        // Mock the ZipFile object
        mockZipFile = mock(ZipFile.class);

        // Mock the InputStream
        String testData = "This is a test";
        byte[] testDataBytes = testData.getBytes();
        mockInputStream = new ByteArrayInputStream(testDataBytes);

        // Mock the behavior of ZipFile methods
        when(mockZipFile.getEntry("test.txt")).thenReturn(new ZipEntry("test.txt"));
        when(mockZipFile.getInputStream(any(ZipEntry.class))).thenReturn(mockInputStream);

        // Create the ZipFileProcessor instance with the mocked ZipFile
        zipFileProcessor = new ZipFileProcessor(mockZipFile);
    }

    @Test
    void testGetZknFileXML() {
        try {
            ZipFile zipFile = new ZipFile(new File("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/firstzettel.zkn3"));
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(zipFile);

            ZipEntry zknFileXML = zipFileProcessor.getZknFileXML();

            assertNotNull(zknFileXML, "Zettelkasten XML should not be null");
            // Add more assertions as needed
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUnmarshal() throws JAXBException, IOException, javax.xml.bind.JAXBException {
        ZipFile zipFile = new ZipFile(new File("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"));
        ZipFileProcessor zipFileProcessor = new ZipFileProcessor(zipFile);
        Zettelkasten zettelkasten = zipFileProcessor.unmarshall();
    }
}
