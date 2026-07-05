package zk.source.zkn3;

import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.importing.Zkn3NoteRecord;
import zk.core.ports.Zkn3SourceReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class Zkn3DomSourceReader implements Zkn3SourceReader {
    private static final String ZKN_FILE_ENTRY = "zknFile.xml";
    private static final String EXPECTED_ROOT = "zettelkasten";
    private static final String ZETTEL_ELEMENT = "zettel";

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
                return extractNoteRecords(zkn3File, root);
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
                    ZKN_FILE_ENTRY,
                    "Could not parse zknFile.xml root element: " + e.getMessage()
            );
        }
    }

    private static Zkn3ImportBatch extractNoteRecords(Path zkn3File, Element root) {
        List<Zkn3NoteRecord> notes = new ArrayList<>();
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    extractNoteRecord(zkn3File, element, notes, diagnostics);
                }
            }
        }

        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                ZETTEL_ELEMENT,
                "Extracted "
                        + notes.size()
                        + " ZKN3 note records; keyword, link, manual-link, and sequence mapping not implemented yet."
        ));

        return new Zkn3ImportBatch(notes, List.of(), List.of(), List.of(), diagnostics);
    }

    private static void extractNoteRecord(
            Path zkn3File,
            Element zettel,
            List<Zkn3NoteRecord> notes,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        String sourceId = zettel.getAttribute("zknid").trim();
        if (sourceId.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    zkn3File.toString(),
                    "zknid",
                    "Missing required zknid attribute; skipped zettel."
            ));
            return;
        }

        Optional<Instant> createdAt = parseTimestamp(zettel.getAttribute("ts_created"));
        if (createdAt.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    "ts_created",
                    "Missing or malformed ts_created timestamp; skipped zettel."
            ));
            return;
        }

        Optional<Instant> modifiedAt = parseTimestamp(zettel.getAttribute("ts_edited"));
        if (modifiedAt.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.WARNING,
                    sourceId,
                    "ts_edited",
                    "Missing or malformed ts_edited timestamp; skipped zettel."
            ));
            return;
        }

        Optional<String> title = directChildText(zettel, "title");
        if (title.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.WARNING,
                    sourceId,
                    "title",
                    "Missing title element; using empty title."
            ));
        }

        Optional<String> content = directChildText(zettel, "content");
        if (content.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.WARNING,
                    sourceId,
                    "content",
                    "Missing content element; using empty body."
            ));
        }

        RatingParseResult rating = parseRating(zettel.getAttribute("rating"));
        if (rating.malformed()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.WARNING,
                    sourceId,
                    "rating",
                    "Malformed rating value; using empty rating."
            ));
        }

        notes.add(new Zkn3NoteRecord(
                sourceId,
                title.orElse(""),
                content.orElse(""),
                createdAt.get(),
                modifiedAt.get(),
                rating.value()
        ));
    }

    private static Optional<String> directChildText(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (childName.equals(element.getTagName())) {
                    return Optional.of(element.getTextContent());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Instant> parseTimestamp(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || !trimmed.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }

        try {
            long timestamp = Long.parseLong(trimmed);
            if (trimmed.length() >= 13) {
                return Optional.of(Instant.ofEpochMilli(timestamp));
            }

            return Optional.of(Instant.ofEpochSecond(timestamp));
        } catch (NumberFormatException | DateTimeException e) {
            return Optional.empty();
        }
    }

    private static RatingParseResult parseRating(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return new RatingParseResult(OptionalInt.empty(), false);
        }

        try {
            return new RatingParseResult(OptionalInt.of(Integer.parseInt(trimmed)), false);
        } catch (NumberFormatException e) {
            return new RatingParseResult(OptionalInt.empty(), true);
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
        return emptyBatchWithDiagnostic(zkn3File, severity, ZKN_FILE_ENTRY, message);
    }

    private static Zkn3ImportBatch emptyBatchWithDiagnostic(
            Path zkn3File,
            Zkn3DiagnosticSeverity severity,
            String field,
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
                        field,
                        message
                ))
        );
    }

    private record RatingParseResult(OptionalInt value, boolean malformed) {
    }
}
