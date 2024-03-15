package ch.dreyeck.zettelkasten.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileProcessor {
    private ZipFile zipFile;
    private String processedData;

    public ZipFileProcessor(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    public void processZipFile() throws IOException {
        // Process the zip file
        // For demonstration, let's assume we read the contents of "test.txt" and store it in processedData
        ZipEntry entry = zipFile.getEntry("test.txt");
        if (entry != null) {
            InputStream inputStream = zipFile.getInputStream(entry);
            byte[] buffer = new byte[1024];
            StringBuilder stringBuilder = new StringBuilder();
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, bytesRead));
            }
            processedData = stringBuilder.toString();
        }
    }

    public String getProcessedData() {
        return processedData;
    }
}
