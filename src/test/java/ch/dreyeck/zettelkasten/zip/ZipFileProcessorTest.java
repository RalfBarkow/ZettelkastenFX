import ch.dreyeck.zettelkasten.zip.ZipFileProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZipFileProcessorTest {
    private ZipFileProcessor zipFileProcessor;
    private ZipFile zipFile;
    private String testData = "This is a test";

    @BeforeEach
    public void setUp() {
        zipFile = mock(ZipFile.class);
        zipFileProcessor = new ZipFileProcessor();
    }

    @Test
    public void testProcessZipFile() throws IOException {
        Enumeration<? extends ZipEntry> mockEnumeration = mock(Enumeration.class);
        // Adjusted the type of enumerationOngoingStubbing to match the wildcard capture
        OngoingStubbing<? extends Enumeration<? extends ZipEntry>> enumerationOngoingStubbing;

        // Mock ZipEntry
        ZipEntry zipEntry = new ZipEntry("test.txt");

        // Mock the behavior of mockEnumeration
        when(mockEnumeration.hasMoreElements()).thenReturn(true).thenReturn(false);
        when(mockEnumeration.nextElement()).thenReturn(zipEntry);

        // Mock InputStream for the ZipEntry
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
        when(zipFile.getInputStream(zipEntry)).thenReturn(inputStream);

        // Test ZipFileProcessor
        zipFileProcessor.processZipFile("/tmp/file.zip");

        // Assert that the processed data matches the test data
        assertEquals(testData, zipFileProcessor.getProcessedData());
    }
}
