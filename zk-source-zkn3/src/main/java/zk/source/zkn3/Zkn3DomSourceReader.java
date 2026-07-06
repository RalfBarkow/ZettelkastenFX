package zk.source.zkn3;

import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.importing.Zkn3KeywordRecord;
import zk.core.importing.Zkn3LinkKind;
import zk.core.importing.Zkn3LinkRecord;
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
    private static final String LINKS_ELEMENT = "links";
    private static final String LINK_ELEMENT = "link";
    private static final String MANLINKS_ELEMENT = "manlinks";
    private static final String LUHMANN_ELEMENT = "luhmann";
    private static final String INCOMPLETE_BATCH_MESSAGE =
            "ZKN3 note batch is incomplete and rejected; no note records extracted.";
    private static final String INCOMPLETE_IMPORT_BATCH_MESSAGE =
            "ZKN3 import batch is incomplete and rejected.";
    private static final String UNSUPPORTED_ATTACHMENT_MESSAGE =
            "ZKN3 attachment or hyperlink metadata is present but no attachment import record exists yet; "
                    + "complete import batch rejected.";

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
            KeywordResolutionResult resolution = resolveKeywordRecords(zknRoot, keywordEntries);
            if (!resolution.diagnostics().isEmpty()) {
                return rejectedBatchWithKeywordReferenceDiagnostics(zkn3File, resolution.diagnostics());
            }

            Optional<Zkn3ImportDiagnostic> unsupportedAttachment = firstUnsupportedAttachmentDiagnostic(zknRoot);
            if (unsupportedAttachment.isPresent()) {
                return rejectedBatchWithUnsupportedAttachmentDiagnostic(zkn3File, unsupportedAttachment.get());
            }

            ManualLinkResolutionResult manualLinks = resolveManualLinks(zknRoot);
            if (hasErrorDiagnostic(manualLinks.diagnostics())) {
                return rejectedBatchWithManualLinkDiagnostics(zkn3File, manualLinks.diagnostics());
            }

            LuhmannResolutionResult luhmann = resolveLuhmannSequences(zknRoot);
            if (hasErrorDiagnostic(luhmann.diagnostics())) {
                return rejectedBatchWithLuhmannDiagnostics(zkn3File, luhmann.diagnostics());
            }

            return noteBatchWithKeywordFileDiagnostics(
                    zkn3File,
                    noteBatch,
                    keywordEntries.size(),
                    resolution.keywordRecords(),
                    manualLinks,
                    luhmann
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

    private static KeywordResolutionResult resolveKeywordRecords(Element zknRoot, List<String> keywordEntries) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        List<Zkn3KeywordRecord> keywordRecords = new ArrayList<>();

        NodeList children = zknRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    KeywordResolutionResult result = resolveZettelKeywordRecords(
                            element,
                            keywordEntries
                    );
                    diagnostics.addAll(result.diagnostics());
                    keywordRecords.addAll(result.keywordRecords());
                }
            }
        }

        if (!diagnostics.isEmpty()) {
            return new KeywordResolutionResult(List.of(), diagnostics);
        }

        return new KeywordResolutionResult(keywordRecords, diagnostics);
    }

    private static KeywordResolutionResult resolveZettelKeywordRecords(Element zettel, List<String> keywordEntries) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        String sourceId = zettel.getAttribute("zknid").trim();
        Optional<String> keywords = directChildText(zettel, EXPECTED_KEYWORD_ROOT);
        if (keywords.isEmpty() || keywords.get().trim().isEmpty()) {
            return new KeywordResolutionResult(List.of(), diagnostics);
        }

        Set<String> resolvedKeywords = new LinkedHashSet<>();
        String[] tokens = keywords.get().split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                resolveKeywordToken(sourceId, trimmed, keywordEntries, resolvedKeywords, diagnostics);
            }
        }

        if (!diagnostics.isEmpty()) {
            return new KeywordResolutionResult(List.of(), diagnostics);
        }

        List<Zkn3KeywordRecord> keywordRecords = resolvedKeywords.stream()
                .map(keyword -> new Zkn3KeywordRecord(sourceId, keyword))
                .toList();
        return new KeywordResolutionResult(keywordRecords, diagnostics);
    }

    private static ManualLinkResolutionResult resolveManualLinks(Element zknRoot) {
        List<Element> zettels = zettelElements(zknRoot);
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        Set<Zkn3LinkRecord> resolvedLinks = new LinkedHashSet<>();

        for (Element source : zettels) {
            String sourceId = source.getAttribute("zknid").trim();
            Optional<String> manlinks = directChildText(source, MANLINKS_ELEMENT);
            if (manlinks.isEmpty() || manlinks.get().trim().isEmpty()) {
                continue;
            }

            String[] tokens = manlinks.get().split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    resolveManualLinkToken(sourceId, trimmed, zettels, resolvedLinks, diagnostics);
                }
            }
        }

        if (diagnostics.stream().anyMatch(diagnostic -> Zkn3DiagnosticSeverity.ERROR == diagnostic.severity())) {
            return new ManualLinkResolutionResult(List.of(), diagnostics);
        }

        return new ManualLinkResolutionResult(new ArrayList<>(resolvedLinks), diagnostics);
    }

    private static LuhmannResolutionResult resolveLuhmannSequences(Element zknRoot) {
        List<Element> zettels = zettelElements(zknRoot);
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>();
        Set<String> resolvedReferences = new LinkedHashSet<>();

        for (Element source : zettels) {
            String sourceId = source.getAttribute("zknid").trim();
            Optional<String> luhmann = directChildText(source, LUHMANN_ELEMENT);
            if (luhmann.isEmpty() || luhmann.get().trim().isEmpty()) {
                continue;
            }

            String[] tokens = luhmann.get().split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    resolveLuhmannToken(sourceId, trimmed, zettels, resolvedReferences, diagnostics);
                }
            }
        }

        if (hasErrorDiagnostic(diagnostics)) {
            return new LuhmannResolutionResult(0, diagnostics);
        }

        return new LuhmannResolutionResult(resolvedReferences.size(), diagnostics);
    }

    private static List<Element> zettelElements(Element zknRoot) {
        List<Element> zettels = new ArrayList<>();
        NodeList children = zknRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    zettels.add(element);
                }
            }
        }
        return zettels;
    }

    private static void resolveManualLinkToken(
            String sourceId,
            String token,
            List<Element> zettels,
            Set<Zkn3LinkRecord> resolvedLinks,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        int manualLinkIndex;
        try {
            manualLinkIndex = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    MANLINKS_ELEMENT,
                    "Invalid manual link token '"
                            + token
                            + "'; expected one-based integer zettel entry reference."
            ));
            return;
        }

        if (manualLinkIndex <= 0) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    MANLINKS_ELEMENT,
                    "Invalid manual link index "
                            + manualLinkIndex
                            + "; zettel entry positions are one-based and must be greater than zero."
            ));
            return;
        }

        int targetIndex = manualLinkIndex - 1;
        if (targetIndex >= zettels.size()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    MANLINKS_ELEMENT,
                    "Manual link index "
                            + manualLinkIndex
                            + " is out of range for zknFile.xml with "
                            + zettels.size()
                            + " zettel entries."
            ));
            return;
        }

        String targetId = zettels.get(targetIndex).getAttribute("zknid").trim();
        if (targetId.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    sourceId,
                    MANLINKS_ELEMENT,
                    "Manual link index "
                            + manualLinkIndex
                            + " resolves to a target zettel without a source id."
            ));
            return;
        }

        Zkn3LinkRecord linkRecord = new Zkn3LinkRecord(sourceId, targetId, Zkn3LinkKind.MANUAL);
        boolean firstResolvedReference = resolvedLinks.add(linkRecord);
        if (firstResolvedReference && sourceId.equals(targetId)) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.WARNING,
                    sourceId,
                    MANLINKS_ELEMENT,
                    "Manual link resolves to the source note itself; preserving explicit self-link."
            ));
        }
    }

    private static void resolveLuhmannToken(
            String parentId,
            String token,
            List<Element> zettels,
            Set<String> resolvedReferences,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        int luhmannIndex;
        try {
            luhmannIndex = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    parentId,
                    LUHMANN_ELEMENT,
                    "Invalid Luhmann sequence token '"
                            + token
                            + "'; expected one-based integer zettel entry reference."
            ));
            return;
        }

        if (luhmannIndex <= 0) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    parentId,
                    LUHMANN_ELEMENT,
                    "Invalid Luhmann sequence index "
                            + luhmannIndex
                            + "; zettel entry positions are one-based and must be greater than zero."
            ));
            return;
        }

        int childIndex = luhmannIndex - 1;
        if (childIndex >= zettels.size()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    parentId,
                    LUHMANN_ELEMENT,
                    "Luhmann sequence index "
                            + luhmannIndex
                            + " is out of range for zknFile.xml with "
                            + zettels.size()
                            + " zettel entries."
            ));
            return;
        }

        String childId = zettels.get(childIndex).getAttribute("zknid").trim();
        if (childId.isEmpty()) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    parentId,
                    LUHMANN_ELEMENT,
                    "Luhmann sequence index "
                            + luhmannIndex
                            + " resolves to a child zettel without a source id."
            ));
            return;
        }

        if (parentId.equals(childId)) {
            diagnostics.add(new Zkn3ImportDiagnostic(
                    Zkn3DiagnosticSeverity.ERROR,
                    parentId,
                    LUHMANN_ELEMENT,
                    "Luhmann sequence reference resolves to the parent note itself; "
                            + "complete import batch rejected."
            ));
            return;
        }

        resolvedReferences.add(parentId + "->" + childId);
    }

    private static Optional<Zkn3ImportDiagnostic> firstUnsupportedAttachmentDiagnostic(Element zknRoot) {
        NodeList children = zknRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (ZETTEL_ELEMENT.equals(element.getTagName())) {
                    Optional<Zkn3ImportDiagnostic> diagnostic = unsupportedAttachmentDiagnostic(element);
                    if (diagnostic.isPresent()) {
                        return diagnostic;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Zkn3ImportDiagnostic> unsupportedAttachmentDiagnostic(Element zettel) {
        NodeList children = zettel.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (LINKS_ELEMENT.equals(element.getTagName()) && hasNonblankLinkChild(element)) {
                    return Optional.of(new Zkn3ImportDiagnostic(
                            Zkn3DiagnosticSeverity.ERROR,
                            zettel.getAttribute("zknid").trim(),
                            LINKS_ELEMENT + "/" + LINK_ELEMENT,
                            UNSUPPORTED_ATTACHMENT_MESSAGE
                    ));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean hasNonblankLinkChild(Element links) {
        NodeList children = links.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (LINK_ELEMENT.equals(element.getTagName()) && !element.getTextContent().trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void resolveKeywordToken(
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

    private static boolean hasErrorDiagnostic(List<Zkn3ImportDiagnostic> diagnostics) {
        return diagnostics.stream()
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

    private static Zkn3ImportBatch rejectedBatchWithUnsupportedAttachmentDiagnostic(
            Path zkn3File,
            Zkn3ImportDiagnostic diagnostic
    ) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        diagnostic,
                        new Zkn3ImportDiagnostic(
                                Zkn3DiagnosticSeverity.ERROR,
                                zkn3File.toString(),
                                "import",
                                INCOMPLETE_IMPORT_BATCH_MESSAGE
                        )
                )
        );
    }

    private static Zkn3ImportBatch rejectedBatchWithManualLinkDiagnostics(
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

    private static Zkn3ImportBatch rejectedBatchWithLuhmannDiagnostics(
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
            List<Zkn3KeywordRecord> keywordRecords,
            ManualLinkResolutionResult manualLinks,
            LuhmannResolutionResult luhmann
    ) {
        List<Zkn3ImportDiagnostic> diagnostics = new ArrayList<>(noteBatch.diagnostics());
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                KEYWORD_FILE_ENTRY,
                "Validated keywordFile.xml root keywords with "
                        + entryCount
                        + " entry elements."
        ));
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                EXPECTED_KEYWORD_ROOT,
                "Extracted "
                        + keywordRecords.size()
                        + " ZKN3 keyword records for "
                        + noteBatch.notes().size()
                        + " notes."
        ));
        diagnostics.addAll(manualLinks.diagnostics());
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                MANLINKS_ELEMENT,
                "Extracted "
                        + manualLinks.linkRecords().size()
                        + " ZKN3 manual link records for "
                        + noteBatch.notes().size()
                        + " notes."
        ));
        diagnostics.addAll(luhmann.diagnostics());
        diagnostics.add(new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.INFO,
                zkn3File.toString(),
                LUHMANN_ELEMENT,
                "Resolved "
                        + luhmann.resolvedReferenceCount()
                        + " Luhmann sequence references for "
                        + noteBatch.notes().size()
                        + " parent notes; sequence record mapping not implemented yet."
        ));

        return new Zkn3ImportBatch(
                noteBatch.notes(),
                keywordRecords,
                manualLinks.linkRecords(),
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
                        + " ZKN3 note records; attachment-link and sequence mapping not implemented yet."
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

    private record KeywordResolutionResult(
            List<Zkn3KeywordRecord> keywordRecords,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
    }

    private record ManualLinkResolutionResult(
            List<Zkn3LinkRecord> linkRecords,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
    }

    private record LuhmannResolutionResult(
            int resolvedReferenceCount,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
    }
}
