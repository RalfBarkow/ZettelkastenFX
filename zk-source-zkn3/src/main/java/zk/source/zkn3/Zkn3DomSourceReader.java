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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class Zkn3DomSourceReader implements Zkn3SourceReader {
    private static final String ZKN_FILE_ENTRY = "zknFile.xml";
    private static final String KEYWORD_FILE_ENTRY = "keywordFile.xml";
    private static final String EXPECTED_ROOT = "zettelkasten";
    private static final String EXPECTED_KEYWORD_ROOT = "keywords";
    private static final String KEYWORD_ENTRY_ELEMENT = "entry";
    private static final String ZETTEL_ELEMENT = "zettel";
    private static final String INCOMPLETE_BATCH_MESSAGE =
            "ZKN3 note batch is incomplete and rejected; no note records extracted.";
    private static final String INCOMPLETE_IMPORT_BATCH_MESSAGE =
            "ZKN3 import batch is incomplete and rejected.";

    @Override
    public Zkn3ImportBatch read(Path zkn3File) throws IOException {
        Objects.requireNonNull(zkn3File, "zkn3File");

        try (ZipFile zipFile = new ZipFile(zkn3File.toFile())) {
            ZipEntry zknFile = zipFile.getEntry(ZKN_FILE_ENTRY);
            if (zknFile != null) {
                return probeZknFileRoot(zkn3File, zipFile, zknFile, zipFile.getEntry(KEYWORD_FILE_ENTRY));
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
            ZipEntry zknFile,
            ZipEntry keywordFile
    ) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(zknFile)) {
            DocumentBuilder documentBuilder = newSecureDocumentBuilderFactory().newDocumentBuilder();
            documentBuilder.setErrorHandler(new ThrowingErrorHandler());
            Document document = documentBuilder.parse(inputStream);
            Element root = document.getDocumentElement();
            String rootName = root == null ? "" : root.getTagName();

            if (EXPECTED_ROOT.equals(rootName)) {
                Zkn3ImportBatch noteBatch = extractNoteRecords(zkn3File, root);
                if (hasErrorDiagnostic(noteBatch)) {
                    return noteBatch;
                }

                if (keywordFile == null) {
                    return rejectedBatchWithMissingKeywordFileDiagnostic(zkn3File);
                }

                return probeKeywordFileRoot(zkn3File, zipFile, keywordFile, root, noteBatch);
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

    private static Zkn3ImportBatch probeKeywordFileRoot(
            Path zkn3File,
            ZipFile zipFile,
            ZipEntry keywordFile,
            Element zknRoot,
            Zkn3ImportBatch noteBatch
    ) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(keywordFile)) {
            DocumentBuilder documentBuilder = newSecureDocumentBuilderFactory().newDocumentBuilder();
            documentBuilder.setErrorHandler(new ThrowingErrorHandler());
            Document document = documentBuilder.parse(inputStream);
            Element root = document.getDocumentElement();
            String rootName = root == null ? "" : root.getTagName();

            if (!EXPECTED_KEYWORD_ROOT.equals(rootName)) {
                return rejectedBatchWithKeywordFileShapeDiagnostic(
                        zkn3File,
                        "Expected keywordFile.xml root element keywords but found " + rootName + "."
                );
            }

            Optional<String> unexpectedElement = firstUnexpectedKeywordChildElement(root);
            if (unexpectedElement.isPresent()) {
                return rejectedBatchWithKeywordFileShapeDiagnostic(
                        zkn3File,
                        "Expected keywordFile.xml child element entry but found " + unexpectedElement.get() + "."
                );
            }

            List<String> keywordEntries = keywordEntries(root);
            KeywordResolutionProbeResult resolution = probeKeywordReferences(zknRoot, keywordEntries);
            if (!resolution.diagnostics().isEmpty()) {
                return rejectedBatchWithKeywordReferenceDiagnostics(zkn3File, resolution.diagnostics());
            }

            return noteBatchWithKeywordFileDiagnostics(
                    zkn3File,
                    noteBatch,
                    keywordEntries.size(),
                    resolution.resolvedReferenceCount()
            );
        } catch (ParserConfigurationException | SAXException e) {
            return rejectedBatchWithMalformedKeywordFileDiagnostic(zkn3File, e);
        }
    }

    private static Optional<String> firstUnexpectedKeywordChildElement(Element root) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (!KEYWORD_ENTRY_ELEMENT.equals(element.getTagName())) {
                    return Optional.of(element.getTagName());
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> keywordEntries(Element root) {
        List<String> entries = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (KEYWORD_ENTRY_ELEMENT.equals(element.getTagName())) {
                    entries.add(element.getTextContent());
                }
            }
        }
        return entries;
    }

    private static KeywordResolutionProbeResult probeKeywordReferences(Element zknRoot, List<String> keywordEntries) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        int resolvedReferenceCount = 0;

        NodeList children = zknRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    KeywordResolutionProbeResult result = probeZettelKeywordReferences(
                            element,
                            keywordEntries
                    );
                    diagnostics.addAll(result.diagnostics());
                    resolvedReferenceCount += result.resolvedReferenceCount();
                }
            }
        }

        return new KeywordResolutionProbeResult(resolvedReferenceCount, diagnostics);
    }

    private static KeywordResolutionProbeResult probeZettelKeywordReferences(Element zettel, List<String> keywordEntries) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        String sourceId = zettel.getAttribute("zknid").trim();
        Optional<String> keywords = directChildText(zettel, EXPECTED_KEYWORD_ROOT);
        if (keywords.isEmpty() || keywords.get().trim().isEmpty()) {
            return new KeywordResolutionProbeResult(0, diagnostics);
        }

        Set<String> resolvedKeywords = new LinkedHashSet<>();
        String[] tokens = keywords.get().split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                probeKeywordToken(sourceId, trimmed, keywordEntries, resolvedKeywords, diagnostics);
            }
        }

        if (!diagnostics.isEmpty()) {
            return new KeywordResolutionProbeResult(0, diagnostics);
        }

        return new KeywordResolutionProbeResult(resolvedKeywords.size(), diagnostics);
    }

    private static void probeKeywordToken(
            String sourceId,
            String token,
            List<String> keywordEntries,
            Set<String> resolvedKeywords,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        int keywordIndex;
        try {
            keywordIndex = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    EXPECTED_KEYWORD_ROOT,
                    "Invalid keyword index token '"
                            + token
                            + "'; expected one-based integer reference into keywordFile.xml."
            ));
            return;
        }

        if (keywordIndex <= 0) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    EXPECTED_KEYWORD_ROOT,
                    "Invalid keyword index "
                            + keywordIndex
                            + "; keyword indexes are one-based and must be greater than zero."
            ));
            return;
        }

        int entryIndex = keywordIndex - 1;
        if (entryIndex >= keywordEntries.size()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    EXPECTED_KEYWORD_ROOT,
                    "Keyword index "
                            + keywordIndex
                            + " is out of range for keywordFile.xml with "
                            + keywordEntries.size()
                            + " entries."
            ));
            return;
        }

        String keyword = keywordEntries.get(entryIndex);
        if (keyword.trim().isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    KEYWORD_FILE_ENTRY,
                    "Keyword index "
                            + keywordIndex
                            + " resolves to a blank keyword entry."
            ));
            return;
        }

        resolvedKeywords.add(keyword);
    }

    private static boolean hasErrorDiagnostic(Zkn3ImportBatch batch) {
        return batch.diagnostics().stream()
                .anyMatch(diagnostic -> Zkn3DiagnosticSeverity.ERROR == diagnostic.severity());
    }

    private static Zkn3ImportBatch rejectedBatchWithMissingKeywordFileDiagnostic(Path zkn3File) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                KEYWORD_FILE_ENTRY,
                                "Missing required keywordFile.xml entry in ZKN3 container; "
                                        + "keyword-aware import batch rejected."
                        ),
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                "import",
                                INCOMPLETE_IMPORT_BATCH_MESSAGE
                        )
                )
        );
    }

    private static Zkn3ImportBatch rejectedBatchWithMalformedKeywordFileDiagnostic(Path zkn3File, Exception exception) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                KEYWORD_FILE_ENTRY,
                                "Could not parse keywordFile.xml root element: " + exception.getMessage()
                        ),
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                "import",
                                INCOMPLETE_IMPORT_BATCH_MESSAGE
                        )
                )
        );
    }

    private static Zkn3ImportBatch rejectedBatchWithKeywordFileShapeDiagnostic(Path zkn3File, String message) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                KEYWORD_FILE_ENTRY,
                                message
                        ),
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                "import",
                                INCOMPLETE_IMPORT_BATCH_MESSAGE
                        )
                )
        );
    }

    private static Zkn3ImportBatch rejectedBatchWithKeywordReferenceDiagnostics(
            Path zkn3File,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        List<Zkn3ImportDiagnostic> rejectedDiagnostics = new ArrayList<>(diagnostics);
        rejectedDiagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.ERROR,
                zkn3File.toString(),
                "import",
                INCOMPLETE_IMPORT_BATCH_MESSAGE
        ));

        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                rejectedDiagnostics
        );
    }

    private static Zkn3ImportBatch noteBatchWithKeywordFileDiagnostics(
            Path zkn3File,
            Zkn3ImportBatch noteBatch,
            int entryCount,
            int resolvedReferenceCount
    ) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>(noteBatch.diagnostics());
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                KEYWORD_FILE_ENTRY,
                "Validated keywordFile.xml root keywords with "
                        + entryCount
                        + " entry elements; keyword mapping not implemented yet."
        ));
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                EXPECTED_KEYWORD_ROOT,
                "Resolved "
                        + resolvedReferenceCount
                        + " keyword references for "
                        + noteBatch.notes().size()
                        + " notes; keyword record mapping not implemented yet."
        ));

        return new Zkn3ImportBatch(
                noteBatch.notes(),
                List.of(),
                List.of(),
                List.of(),
                diagnostics
        );
    }

    private static Zkn3ImportBatch extractNoteRecords(Path zkn3File, Element root) {
        List<Zkn3NoteRecord> notes = new ArrayList<>();
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        boolean incompleteBatch = false;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    NoteExtractionResult result = extractNoteRecord(zkn3File, element, diagnostics);
                    if (result.record().isPresent()) {
                        notes.add(result.record().get());
                    }
                    incompleteBatch = incompleteBatch || result.incompleteBatch();
                }
            }
        }

        if (incompleteBatch) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    zkn3File.toString(),
                    ZETTEL_ELEMENT,
                    INCOMPLETE_BATCH_MESSAGE
            ));

            return new Zkn3ImportBatch(List.of(), List.of(), List.of(), List.of(), diagnostics);
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

    private static NoteExtractionResult extractNoteRecord(
            Path zkn3File,
            Element zettel,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        String sourceId = zettel.getAttribute("zknid").trim();
        if (sourceId.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    zkn3File.toString(),
                    "zknid",
                    "Missing required zknid attribute."
            ));
            return new NoteExtractionResult(Optional.empty(), true);
        }

        Optional<Instant> createdAt = parseTimestamp(zettel.getAttribute("ts_created"));
        if (createdAt.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    "ts_created",
                    "Missing or malformed ts_created timestamp."
            ));
            return new NoteExtractionResult(Optional.empty(), true);
        }

        Optional<Instant> modifiedAt = parseTimestamp(zettel.getAttribute("ts_edited"));
        if (modifiedAt.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    "ts_edited",
                    "Missing or malformed ts_edited timestamp."
            ));
            return new NoteExtractionResult(Optional.empty(), true);
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

        Zkn3NoteRecord record = new Zkn3NoteRecord(
                sourceId,
                title.orElse(""),
                content.orElse(""),
                createdAt.get(),
                modifiedAt.get(),
                rating.value()
        );
        return new NoteExtractionResult(Optional.of(record), false);
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

    private record NoteExtractionResult(Optional<Zkn3NoteRecord> record, boolean incompleteBatch) {
    }

    private record KeywordResolutionProbeResult(
            int resolvedReferenceCount,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
    }
}
