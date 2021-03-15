package ch.dreyeck.zettelkasten.input;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.util.zip.ZipInputStream;

public class Zettelkasten {
    // TODO Unmarshalling from an InputStream:
    InputStream inputStream = new FileInputStream("zknFile.xml");
    JAXBContext jaxbContext = JAXBContext.newInstance(ch.dreyeck.zettelkasten.xml.Zettelkasten.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    Object object = unmarshaller.unmarshal(inputStream);

    public Zettelkasten() throws FileNotFoundException, JAXBException {
    }

    OutputStream getZknFile(ZipInputStream zipInputStream){
        OutputStream zknFile = null;

        return zknFile;
    }
}
