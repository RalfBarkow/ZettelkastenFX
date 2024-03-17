package ch.dreyeck.zettelkasten.zip;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileProcessor {
    private final ZipFile zipFile;
    private Zettelkasten processedData;

    public ZipFileProcessor(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    public ZipFile getZipFile() {
        return zipFile;
    }

    public void processZipFile() throws IOException {
        // Process the zip file
        // For demonstration, let's assume we read the contents of "zknFile.xml" and store it in processedData
        ZipEntry entry = zipFile.getEntry("zknFile.xml");
        if (entry != null) processedData = unmarshall(entry);
    }

    public Zettelkasten unmarshall(ZipEntry entry) {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Zettelkasten.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Zettelkasten) unmarshaller.unmarshal(inputStream);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Zettelkasten getProcessedData() {
        return processedData;
    }

    // Special case of processZipFile for zknFile.xml
    //public String getZknFileXML() throws IOException {
    //    processZipFile();
    //    return processedData;
    //}

    public ZipEntry getZknFileXML() throws IOException {
        return zipFile.getEntry("zknFile.xml");
    }

}
