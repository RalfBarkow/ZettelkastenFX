package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.importing.Zkn3NoteRecord;
import zk.core.ports.Zkn3SourceReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3DomSourceReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void implementsSourceReaderPort() {
        assertInstanceOf(Zkn3SourceReader.class, new Zkn3DomSourceReader());
    }

    @Test
    void readReturnsSummaryDiagnosticWhenZknFileHasNoZettelElements() throws IOException {
        Path source = createZip(
                "valid-root.zkn3",
                zipEntry("zknFile.xml", "<zettelkasten/>"),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 0);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readMapsOneValidZettelToNoteRecord() throws IOException {
        Path source = createZip(
                "one-zettel.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="42" ts_created="1700000000000" ts_edited="1700003600" rating="4">
                                    <title>First note</title>
                                    <content>Body &amp; markup [b]raw[/b]</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        Zkn3NoteRecord note = batch.notes().get(0);
        assertEquals("42", note.sourceId());
        assertEquals("First note", note.title());
        assertEquals("Body & markup [b]raw[/b]", note.body());
        assertEquals(Instant.ofEpochMilli(1700000000000L), note.createdAt());
        assertEquals(Instant.ofEpochSecond(1700003600L), note.modifiedAt());
        assertEquals(OptionalInt.of(4), note.rating());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readMapsTwoValidZettelElementsToTwoNoteRecords() throws IOException {
        Path source = createZip(
                "two-zettel.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                  </zettel>
                                  <zettel zknid="2" ts_created="1700000001000" ts_edited="1700000200000" rating="5">
                                    <title>Second</title>
                                    <content>Second body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals("1", batch.notes().get(0).sourceId());
        assertEquals(OptionalInt.empty(), batch.notes().get(0).rating());
        assertEquals("2", batch.notes().get(1).sourceId());
        assertEquals(OptionalInt.of(5), batch.notes().get(1).rating());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 2);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readRejectsBatchWhenKeywordFileEntryIsMissing() throws IOException {
        Path source = createZip(
                "missing-keyword-file.zkn3",
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                          </zettel>
                        </zettelkasten>
                        """
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source.toString(),
                "keywordFile.xml",
                "Missing required keywordFile.xml entry in ZKN3 container; "
                        + "keyword-aware import batch rejected."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordFileXmlIsMalformed() throws IOException {
        Path source = createZip(
                "malformed-keyword-file.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnosticPrefix(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source.toString(),
                "keywordFile.xml",
                "Could not parse keywordFile.xml root element:"
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenOneZettelIsMissingZknid() throws IOException {
        Path source = createZip(
                "missing-zknid.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="valid" ts_created="1700000000" ts_edited="1700000100" rating="1">
                                    <title>Valid</title>
                                    <content>Valid body</content>
                                  </zettel>
                                  <zettel ts_created="1700000000" ts_edited="1700000100" rating="1">
                                    <title>Missing ID</title>
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source.toString(),
                "zknid",
                "Missing required zknid attribute."
        );
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readUsesEmptyTitleWhenTitleElementIsMissingAndEmitsWarningDiagnostic() throws IOException {
        Path source = createZip(
                "missing-title.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="99" ts_created="1700000000" ts_edited="1700000100">
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals("", batch.notes().get(0).title());
        assertEquals("Body", batch.notes().get(0).body());
        assertNoRelationRecords(batch);
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                "99",
                "title",
                "Missing title element; using empty title."
        );
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readUsesEmptyBodyWhenContentElementIsMissingAndEmitsWarningDiagnostic() throws IOException {
        Path source = createZip(
                "missing-content.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="100" ts_created="1700000000" ts_edited="1700000100">
                                    <title>Title</title>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals("Title", batch.notes().get(0).title());
        assertEquals("", batch.notes().get(0).body());
        assertNoRelationRecords(batch);
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                "100",
                "content",
                "Missing content element; using empty body."
        );
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readUsesEmptyRatingWhenRatingIsMalformedAndEmitsWarningDiagnostic() throws IOException {
        Path source = createZip(
                "malformed-rating.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="101" ts_created="1700000000" ts_edited="1700000100" rating="bad">
                                    <title>Title</title>
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(OptionalInt.empty(), batch.notes().get(0).rating());
        assertNoRelationRecords(batch);
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                "101",
                "rating",
                "Malformed rating value; using empty rating."
        );
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileProbeDiagnostic(batch, source, "keywords");
    }

    @Test
    void readRejectsBatchWhenOneZettelHasMalformedCreatedTimestamp() throws IOException {
        Path source = createZip(
                "malformed-created.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="valid" ts_created="1700000000" ts_edited="1700000100" rating="1">
                                    <title>Valid</title>
                                    <content>Valid body</content>
                                  </zettel>
                                  <zettel zknid="102" ts_created="bad" ts_edited="1700000100" rating="1">
                                    <title>Title</title>
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "102",
                "ts_created",
                "Missing or malformed ts_created timestamp."
        );
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenOneZettelIsMissingEditedTimestamp() throws IOException {
        Path source = createZip(
                "missing-edited.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="valid" ts_created="1700000000" ts_edited="1700000100" rating="1">
                                    <title>Valid</title>
                                    <content>Valid body</content>
                                  </zettel>
                                  <zettel zknid="103" ts_created="1700000000" rating="1">
                                    <title>Title</title>
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "103",
                "ts_edited",
                "Missing or malformed ts_edited timestamp."
        );
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenOneZettelHasMalformedEditedTimestamp() throws IOException {
        Path source = createZip(
                "malformed-edited.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="valid" ts_created="1700000000" ts_edited="1700000100" rating="1">
                                    <title>Valid</title>
                                    <content>Valid body</content>
                                  </zettel>
                                  <zettel zknid="104" ts_created="1700000000" ts_edited="bad" rating="1">
                                    <title>Title</title>
                                    <content>Body</content>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords/>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "104",
                "ts_edited",
                "Missing or malformed ts_edited timestamp."
        );
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileRootIsWrong() throws IOException {
        Path source = createZip("wrong-root.zkn3", "zknFile.xml", "<notzettelkasten/>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Expected root element zettelkasten but found notzettelkasten."
        );
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileEntryIsMissing() throws IOException {
        Path source = createZip("missing-zkn-file.zkn3", "other.xml", "<other/>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Missing required zknFile.xml entry in ZKN3 container."
        );
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileXmlIsMalformed() throws IOException {
        Path source = createZip("malformed-zkn-file.zkn3", "zknFile.xml", "<zettelkasten>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnosticPrefix(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Could not parse zknFile.xml root element:"
        );
    }

    @Test
    void readRejectsInvalidZip() throws IOException {
        Path source = tempDir.resolve("invalid.zkn3");
        Files.writeString(source, "not a zip");

        assertThrows(IOException.class, () -> new Zkn3DomSourceReader().read(source));
    }

    @Test
    void readRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new Zkn3DomSourceReader().read(null));
    }

    private Path createZip(String fileName, String entryName, String content) throws IOException {
        return createZip(fileName, zipEntry(entryName, content));
    }

    private Path createZip(String fileName, ZipFixtureEntry... entries) throws IOException {
        Path source = tempDir.resolve(fileName);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(source))) {
            for (ZipFixtureEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return source;
    }

    private static ZipFixtureEntry zipEntry(String name, String content) {
        return new ZipFixtureEntry(name, content);
    }

    private static void assertNoRelationRecords(Zkn3ImportBatch batch) {
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
    }

    private static void assertSummaryDiagnostic(Zkn3ImportBatch batch, Path source, int noteCount) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "zettel",
                "Extracted "
                        + noteCount
                        + " ZKN3 note records; keyword, link, manual-link, and sequence mapping not implemented yet."
        );
    }

    private static void assertKeywordFileProbeDiagnostic(Zkn3ImportBatch batch, Path source, String rootName) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "keywordFile.xml",
                "Found parseable keywordFile.xml root element "
                        + rootName
                        + "; keyword mapping not implemented yet."
        );
    }

    private static void assertRejectedBatchDiagnostic(Zkn3ImportBatch batch, Path source) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source.toString(),
                "zettel",
                "ZKN3 note batch is incomplete and rejected; no note records extracted."
        );
    }

    private static void assertImportRejectedBatchDiagnostic(Zkn3ImportBatch batch, Path source) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source.toString(),
                "import",
                "ZKN3 import batch is incomplete and rejected."
        );
    }

    private static void assertDiagnostic(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            String sourceId,
            String field,
            String message
    ) {
        assertTrue(
                batch.diagnostics().stream().anyMatch(diagnostic ->
                        severity == diagnostic.severity()
                                && sourceId.equals(diagnostic.sourceId())
                                && field.equals(diagnostic.field())
                                && message.equals(diagnostic.message())),
                "Expected diagnostic "
                        + severity
                        + " "
                        + sourceId
                        + " "
                        + field
                        + " "
                        + message
                        + " in "
                        + batch.diagnostics()
        );
    }

    private static void assertDiagnosticPrefix(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            String sourceId,
            String field,
            String messagePrefix
    ) {
        assertTrue(
                batch.diagnostics().stream().anyMatch(diagnostic ->
                        severity == diagnostic.severity()
                                && sourceId.equals(diagnostic.sourceId())
                                && field.equals(diagnostic.field())
                                && diagnostic.message().startsWith(messagePrefix)),
                "Expected diagnostic prefix "
                        + severity
                        + " "
                        + sourceId
                        + " "
                        + field
                        + " "
                        + messagePrefix
                        + " in "
                        + batch.diagnostics()
        );
    }

    private static void assertEmptyBatchWithDiagnostic(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            Path source,
            String field,
            String message
    ) {
        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(1, batch.diagnostics().size());

        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().get(0);
        assertEquals(severity, diagnostic.severity());
        assertEquals(source.toString(), diagnostic.sourceId());
        assertEquals(field, diagnostic.field());
        assertEquals(message, diagnostic.message());
    }

    private static void assertEmptyBatchWithDiagnosticPrefix(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            Path source,
            String field,
            String messagePrefix
    ) {
        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(1, batch.diagnostics().size());

        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().get(0);
        assertEquals(severity, diagnostic.severity());
        assertEquals(source.toString(), diagnostic.sourceId());
        assertEquals(field, diagnostic.field());
        assertEquals(messagePrefix, diagnostic.message().substring(0, messagePrefix.length()));
    }

    private record ZipFixtureEntry(String name, String content) {
    }
}
