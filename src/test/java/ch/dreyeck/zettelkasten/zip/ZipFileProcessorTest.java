package ch.dreyeck.zettelkasten.zip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testProcessZipFile() throws IOException {
        // Process the zip file
        zipFileProcessor.processZipFile();

        // Assert that the processed data matches the test data
        assertEquals("This is a test", zipFileProcessor.getProcessedData());
    }

    @Test
    void testGetZknFileXML() {
        try {
            ZipFile zipFile = new ZipFile(new File("/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"));
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(zipFile);

            ZipEntry zknFileXML = zipFileProcessor.getZknFileXML();

            assertNotNull(zknFileXML, "Zettelkasten XML should not be null");
            // Add more assertions as needed
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
