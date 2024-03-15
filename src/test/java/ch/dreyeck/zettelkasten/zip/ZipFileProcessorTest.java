package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.zip.ZipFileProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
