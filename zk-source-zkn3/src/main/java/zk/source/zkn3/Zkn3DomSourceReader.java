package zk.source.zkn3;

import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.ports.Zkn3SourceReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class Zkn3DomSourceReader implements Zkn3SourceReader {
    private static final String ZKN_FILE_ENTRY = "zknFile.xml";
    private static final String EXPECTED_ROOT = "zettelkasten";

    @Override
    public Zkn3ImportBatch read(Path zkn3File) throws IOException {
        Objects.requireNonNull(zkn3File, "zkn3File");

        try (ZipFile zipFile = new ZipFile(zkn3File.toFile())) {
            ZipEntry zknFile = zipFile.getEntry(ZKN_FILE_ENTRY);
            if (zknFile != null) {
                return probeZknFileRoot(zkn3File, zipFile, zknFile);
            }

            return emptyBatchWithDiagnostic(
                    zkn3File,
                    Zkn3DiagnosticSeverity.ERROR,
                    "Missing required zknFile.xml entry in ZKN3 container."
            );
        }
    }

    private static Zkn3ImportBatch probeZknFileRoot(
            Path zkn3File,
            ZipFile zipFile,
            ZipEntry zknFile
    ) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(zknFile)) {
            DocumentBuilder documentBuilder = newSecureDocumentBuilderFactory().newDocumentBuilder();
            documentBuilder.setErrorHandler(new ThrowingErrorHandler());
            Document document = documentBuilder.parse(inputStream);
            Element root = document.getDocumentElement();
            String rootName = root == null ? "" : root.getTagName();

            if (EXPECTED_ROOT.equals(rootName)) {
                return emptyBatchWithDiagnostic(
                        zkn3File,
                        Zkn3DiagnosticSeverity.INFO,
                        "Found zknFile.xml root element zettelkasten; zettel mapping not implemented yet."
                );
            }

            return emptyBatchWithDiagnostic(
                    zkn3File,
                    Zkn3DiagnosticSeverity.ERROR,
                    "Expected root element zettelkasten but found " + rootName + "."
            );
        } catch (ParserConfigurationException | SAXException e) {
            return emptyBatchWithDiagnostic(
                    zkn3File,
                    Zkn3DiagnosticSeverity.ERROR,
                    "Could not parse zknFile.xml root element: " + e.getMessage()
            );
        }
    }

    private static DocumentBuilderFactory newSecureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return factory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // Some JDK XML providers do not support every hardening feature.
        }
    }

    private static final class ThrowingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

    private static Zkn3ImportBatch emptyBatchWithDiagnostic(
            Path zkn3File,
            Zkn3DiagnosticSeverity severity,
            String message
    ) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new Zkn3ImportDiagnostic(
                        severity,
                        zkn3File.toString(),
                        ZKN_FILE_ENTRY,
                        message
                ))
        );
    }
}
