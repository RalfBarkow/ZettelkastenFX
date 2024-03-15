package ch.dreyeck.zettelkasten.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileProcessor {
    private StringBuilder processedData;

    public void processZipFile(String zipFilePath) {
        processedData = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if entry is a directory
                if (!entry.isDirectory()) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        // Read and process the entry contents using the inputStream
                        String entryData = processEntry(inputStream);
                        processedData.append(entryData);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String processEntry(InputStream inputStream) throws IOException {
        // Process the InputStream and return the processed data as a String
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    public String getProcessedData() {
        return processedData.toString();
    }
}
