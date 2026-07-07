package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zk.core.importing.Zkn3AttachmentKind;
import zk.core.importing.Zkn3AttachmentRecord;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.importing.Zkn3KeywordRecord;
import zk.core.importing.Zkn3LinkKind;
import zk.core.importing.Zkn3LinkRecord;
import zk.core.importing.Zkn3NoteRecord;
import zk.core.importing.Zkn3SequenceRecord;
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
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 0);
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 0);
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
        assertEquals("1700000000000", note.rawCreatedTimestamp());
        assertEquals("1700003600", note.rawEditedTimestamp());
        assertEquals(Instant.ofEpochMilli(1700000000000L), note.createdAt());
        assertEquals(Instant.ofEpochSecond(1700003600L), note.modifiedAt());
        assertEquals(OptionalInt.of(4), note.rating());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
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
        assertEquals("1700000000", batch.notes().get(0).rawCreatedTimestamp());
        assertEquals("1700000100", batch.notes().get(0).rawEditedTimestamp());
        assertEquals(OptionalInt.empty(), batch.notes().get(0).rating());
        assertEquals("2", batch.notes().get(1).sourceId());
        assertEquals("1700000001000", batch.notes().get(1).rawCreatedTimestamp());
        assertEquals("1700000200000", batch.notes().get(1).rawEditedTimestamp());
        assertEquals(OptionalInt.of(5), batch.notes().get(1).rating());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 2);
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 2);
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
    void readValidatesKeywordFileEntryShapeWhenNotesHaveNoKeywordReferences() throws IOException {
        Path source = createZip(
                "keyword-file-entries.zkn3",
                validZknFileEntry(),
                zipEntry(
                        "keywordFile.xml",
                        """
                                <keywords>
                                  <entry>systems</entry>
                                  <entry>phenomenology</entry>
                                </keywords>
                                """
                )
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 2);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readAllowsKeywordFileEntryFrequencyAttributeWithoutMappingIt() throws IOException {
        Path source = createZip(
                "keyword-file-frequency.zkn3",
                validZknFileEntry(),
                zipEntry("keywordFile.xml", "<keywords><entry f=\"2\">systems</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readIgnoresNonElementKeywordFileChildren() throws IOException {
        Path source = createZip(
                "keyword-file-non-elements.zkn3",
                validZknFileEntry(),
                zipEntry(
                        "keywordFile.xml",
                        """
                                <keywords>
                                  <!-- a legacy export may include comments -->

                                  <entry>systems</entry>
                                  <entry>theory</entry>
                                </keywords>
                                """
                )
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 2);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readMapsOneBasedKeywordReferenceToKeywordRecord() throws IOException {
        Path source = createZip(
                "keyword-reference-one.zkn3",
                validZknFileEntryWithKeywords("1"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readMapsMultipleKeywordReferencesInTokenOrder() throws IOException {
        Path source = createZip(
                "keyword-reference-two.zkn3",
                validZknFileEntryWithKeywords("1,2"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry><entry>beta</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertKeywordRecord(batch.keywords().get(1), "1", "beta");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 2);
        assertKeywordResolutionDiagnostic(batch, source, 2, 1);
    }

    @Test
    void readMapsSameKeywordForTwoNotesAsTwoAssociations() throws IOException {
        Path source = createZip(
                "same-keyword-two-notes.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                  <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                                    <title>Second</title>
                                    <content>Second body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertKeywordRecord(batch.keywords().get(1), "2", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 2);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 2, 2);
    }

    @Test
    void readTrimsWhitespaceAroundKeywordReferenceTokens() throws IOException {
        Path source = createZip(
                "keyword-reference-whitespace.zkn3",
                validZknFileEntryWithKeywords(" 1 , 2 "),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry><entry>beta</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertKeywordRecord(batch.keywords().get(1), "1", "beta");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 2);
        assertKeywordResolutionDiagnostic(batch, source, 2, 1);
    }

    @Test
    void readTreatsBlankKeywordFieldAsNoKeywordReferences() throws IOException {
        Path source = createZip(
                "blank-keywords.zkn3",
                validZknFileEntryWithKeywords("   "),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readTreatsMissingLinksElementAsNoAttachmentMetadata() throws IOException {
        Path source = createZip(
                "missing-links.zkn3",
                validZknFileEntryWithKeywords("1"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readTreatsEmptyLinksElementAsNoAttachmentMetadata() throws IOException {
        Path source = createZip(
                "empty-links.zkn3",
                validZknFileEntryWithKeywordsAndLinks("1", "<links/>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readTreatsBlankLinkValuesAsNoAttachmentMetadata() throws IOException {
        Path source = createZip(
                "blank-link.zkn3",
                validZknFileEntryWithKeywordsAndLinks("1", "<links><link>   </link></links>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(6, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
        assertBlankAttachmentWarning(batch, "1");
    }

    @Test
    void readMapsOneManualLinkReferenceToManualLinkRecord() throws IOException {
        Path source = createZip(
                "manual-link-one.zkn3",
                validTwoZknFileEntryWithFirstManlinks("<manlinks>2</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertKeywordRecord(batch.keywords().get(1), "2", "alpha");
        assertEquals(1, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertEquals(0, batch.sequences().size());
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 2);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 2, 2);
        assertManualLinkRecordDiagnostic(batch, source, 1, 2);
    }

    @Test
    void readMapsMultipleManualLinkReferencesInTokenOrder() throws IOException {
        Path source = createZip(
                "manual-link-two.zkn3",
                validThreeZknFileEntryWithFirstManlinks("<manlinks>2,3</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(3, batch.keywords().size());
        assertEquals(2, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertManualLinkRecord(batch.links().get(1), "1", "3");
        assertEquals(0, batch.sequences().size());
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readTrimsWhitespaceAroundManualLinkReferenceTokensBeforeMappingRecords() throws IOException {
        Path source = createZip(
                "manual-link-whitespace.zkn3",
                validThreeZknFileEntryWithFirstManlinks("<manlinks> 2 , 3 </manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(2, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertManualLinkRecord(batch.links().get(1), "1", "3");
        assertEquals(0, batch.sequences().size());
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readTreatsMissingManlinksFieldAsNoManualLinks() throws IOException {
        Path source = createZip(
                "missing-manlinks.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", ""),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readTreatsBlankManlinksFieldAsNoManualLinks() throws IOException {
        Path source = createZip(
                "blank-manlinks.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", "<manlinks>   </manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readDeduplicatesDuplicateManualLinkReferenceForSameSourceTargetKind() throws IOException {
        Path source = createZip(
                "duplicate-manlink.zkn3",
                validTwoZknFileEntryWithFirstManlinks("<manlinks>2,2</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertEquals(1, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertEquals(0, batch.sequences().size());
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 1, 2);
    }

    @Test
    void readPreservesDifferentSourceNotesLinkingToSameTargetNote() throws IOException {
        Path source = createZip(
                "manual-link-same-target.zkn3",
                validThreeZknFileEntryWithFirstAndSecondManlinks(
                        "<manlinks>3</manlinks>",
                        "<manlinks>3</manlinks>"
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(3, batch.keywords().size());
        assertEquals(2, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "3");
        assertManualLinkRecord(batch.links().get(1), "2", "3");
        assertEquals(0, batch.sequences().size());
        assertEquals(5, batch.diagnostics().size());
        assertManualLinkRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readRejectsCompleteBatchWhenManualLinkTokenIsNotInteger() throws IOException {
        Path source = createZip(
                "manual-link-token-not-integer.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", "<manlinks>abc</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "manlinks",
                "Invalid manual link token 'abc'; expected one-based integer zettel entry reference."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenManualLinkIndexIsZero() throws IOException {
        Path source = createZip(
                "manual-link-token-zero.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", "<manlinks>0</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertInvalidManualLinkIndexDiagnostic(batch, "1", 0);
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenManualLinkIndexIsNegative() throws IOException {
        Path source = createZip(
                "manual-link-token-negative.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", "<manlinks>-1</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertInvalidManualLinkIndexDiagnostic(batch, "1", -1);
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenManualLinkIndexIsOutOfRange() throws IOException {
        Path source = createZip(
                "manual-link-token-out-of-range.zkn3",
                validTwoZknFileEntryWithFirstManlinks("<manlinks>3</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "manlinks",
                "Manual link index 3 is out of range for zknFile.xml with 2 zettel entries."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readMapsSelfLinkWithWarning() throws IOException {
        Path source = createZip(
                "manual-link-self.zkn3",
                validZknFileEntryWithKeywordsAndManlinks("1", "<manlinks>1</manlinks>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertEquals(1, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "1");
        assertEquals(0, batch.sequences().size());
        assertEquals(6, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                "1",
                "manlinks",
                "Manual link resolves to the source note itself; preserving explicit self-link."
        );
        assertManualLinkRecordDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readRejectsManualLinkTargetWithoutZknidBeforeRecordMapping() throws IOException {
        Path source = createZip(
                "manual-link-target-without-zknid.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                    <manlinks>2</manlinks>
                                  </zettel>
                                  <zettel zknid="" ts_created="1700000001" ts_edited="1700000101" rating="">
                                    <title>Missing source id</title>
                                    <content>Second body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
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
    void readRejectsInvalidNoteBeforeManualLinkResolutionProbe() throws IOException {
        Path source = createZip(
                "invalid-note-before-manlinks.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="bad" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                    <manlinks>1</manlinks>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertTimestampDiagnostic(batch, "1", "ts_created", "bad");
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readMapsOneLuhmannReferenceToSequenceRecord() throws IOException {
        Path source = createZip(
                "luhmann-reference-one.zkn3",
                validTwoZknFileEntryWithFirstManlinksAndLuhmann(
                        "<manlinks>2</manlinks>",
                        "<luhmann>2</luhmann>"
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertEquals(1, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertEquals(1, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "2", 0);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 1, 2);
    }

    @Test
    void readMapsMultipleLuhmannReferencesToSequenceRecordsInTokenOrder() throws IOException {
        Path source = createZip(
                "luhmann-reference-two.zkn3",
                validThreeZknFileEntryWithFirstLuhmann("<luhmann>2,3</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(3, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(2, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "2", 0);
        assertSequenceRecord(batch.sequences().get(1), "1", "3", 1);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readTrimsWhitespaceAroundLuhmannReferenceTokens() throws IOException {
        Path source = createZip(
                "luhmann-reference-whitespace.zkn3",
                validThreeZknFileEntryWithFirstLuhmann("<luhmann> 2 , 3 </luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(0, batch.links().size());
        assertEquals(2, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "2", 0);
        assertSequenceRecord(batch.sequences().get(1), "1", "3", 1);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readIgnoresEmptyLuhmannTokensFromExtraDelimiters() throws IOException {
        Path source = createZip(
                "luhmann-reference-empty-tokens.zkn3",
                validTwoZknFileEntryWithFirstLuhmann("<luhmann>,2,,</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(0, batch.links().size());
        assertEquals(1, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "2", 0);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 1, 2);
    }

    @Test
    void readTreatsMissingLuhmannFieldAsNoSequenceReferences() throws IOException {
        Path source = createZip(
                "missing-luhmann.zkn3",
                validZknFileEntryWithKeywords("1"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readTreatsBlankLuhmannFieldAsNoSequenceReferences() throws IOException {
        Path source = createZip(
                "blank-luhmann.zkn3",
                validZknFileEntryWithKeywordsAndLuhmann("1", "<luhmann>   </luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readDeduplicatesDuplicateLuhmannReferenceForSameParentChild() throws IOException {
        Path source = createZip(
                "duplicate-luhmann.zkn3",
                validTwoZknFileEntryWithFirstLuhmann("<luhmann>2,2</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(1, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "2", 0);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 1, 2);
    }

    @Test
    void readPreservesDifferentParentNotesSequencingSameChildNote() throws IOException {
        Path source = createZip(
                "luhmann-same-child.zkn3",
                validThreeZknFileEntryWithFirstAndSecondLuhmann(
                        "<luhmann>3</luhmann>",
                        "<luhmann>3</luhmann>"
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(3, batch.notes().size());
        assertEquals(3, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(2, batch.sequences().size());
        assertSequenceRecord(batch.sequences().get(0), "1", "3", 0);
        assertSequenceRecord(batch.sequences().get(1), "2", "3", 0);
        assertEquals(5, batch.diagnostics().size());
        assertLuhmannSequenceRecordDiagnostic(batch, source, 2, 3);
    }

    @Test
    void readRejectsCompleteBatchWhenLuhmannTokenIsNotInteger() throws IOException {
        Path source = createZip(
                "luhmann-token-not-integer.zkn3",
                validZknFileEntryWithKeywordsAndLuhmann("1", "<luhmann>abc</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "luhmann",
                "Invalid Luhmann sequence token 'abc'; expected one-based integer zettel entry reference."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenLuhmannIndexIsZero() throws IOException {
        Path source = createZip(
                "luhmann-token-zero.zkn3",
                validZknFileEntryWithKeywordsAndLuhmann("1", "<luhmann>0</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertInvalidLuhmannSequenceIndexDiagnostic(batch, "1", 0);
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenLuhmannIndexIsNegative() throws IOException {
        Path source = createZip(
                "luhmann-token-negative.zkn3",
                validZknFileEntryWithKeywordsAndLuhmann("1", "<luhmann>-1</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertInvalidLuhmannSequenceIndexDiagnostic(batch, "1", -1);
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenLuhmannIndexIsOutOfRange() throws IOException {
        Path source = createZip(
                "luhmann-token-out-of-range.zkn3",
                validTwoZknFileEntryWithFirstLuhmann("<luhmann>3</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "luhmann",
                "Luhmann sequence index 3 is out of range for zknFile.xml with 2 zettel entries."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsCompleteBatchWhenLuhmannReferenceIsSelfSequence() throws IOException {
        Path source = createZip(
                "luhmann-self-sequence.zkn3",
                validZknFileEntryWithKeywordsAndLuhmann("1", "<luhmann>1</luhmann>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "luhmann",
                "Luhmann sequence reference resolves to the parent note itself; "
                        + "complete import batch rejected."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsInvalidTargetZettelBeforeLuhmannResolutionProbe() throws IOException {
        Path source = createZip(
                "luhmann-target-without-zknid.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                    <luhmann>2</luhmann>
                                  </zettel>
                                  <zettel zknid="" ts_created="1700000001" ts_edited="1700000101" rating="">
                                    <title>Missing source id</title>
                                    <content>Second body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
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
    void readMapsFileAttachmentToAttachmentRecord() throws IOException {
        Path source = createZip(
                "local-file-attachment.zkn3",
                validZknFileEntryWithKeywordsAndLinks("1", "<links><link>docs/example.pdf</link></links>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(1, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "docs/example.pdf", Zkn3AttachmentKind.FILE, 0);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readMapsUrlAttachmentToAttachmentRecord() throws IOException {
        Path source = createZip(
                "url-attachment.zkn3",
                validZknFileEntryWithKeywordsAndLinks("1", "<links><link>https://example.org/x</link></links>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(1, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "https://example.org/x", Zkn3AttachmentKind.URL, 0);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readMapsUnknownAttachmentKindWithoutRejecting() throws IOException {
        Path source = createZip(
                "unknown-attachment.zkn3",
                validZknFileEntryWithKeywordsAndLinks("1", "<links><link>opaque-reference</link></links>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(1, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "opaque-reference", Zkn3AttachmentKind.UNKNOWN, 0);
        assertEquals(5, batch.diagnostics().size());
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readPreservesMultipleAttachmentOrder() throws IOException {
        Path source = createZip(
                "multiple-attachments.zkn3",
                validZknFileEntryWithKeywordsAndLinks(
                        "1",
                        """
                                <links>
                                  <link>https://example.org/a</link>
                                  <link>docs/example.pdf</link>
                                  <link>opaque-reference</link>
                                </links>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(3, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "https://example.org/a", Zkn3AttachmentKind.URL, 0);
        assertAttachmentRecord(batch.attachments().get(1), "1", "docs/example.pdf", Zkn3AttachmentKind.FILE, 1);
        assertAttachmentRecord(batch.attachments().get(2), "1", "opaque-reference", Zkn3AttachmentKind.UNKNOWN, 2);
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readMapsMixedBlankAndNonblankAttachments() throws IOException {
        Path source = createZip(
                "mixed-attachments.zkn3",
                validZknFileEntryWithKeywordsAndLinks(
                        "1",
                        """
                                <links>
                                  <link>   </link>
                                  <link>https://example.org/a</link>
                                  <link>docs/example.pdf</link>
                                </links>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(2, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "https://example.org/a", Zkn3AttachmentKind.URL, 0);
        assertAttachmentRecord(batch.attachments().get(1), "1", "docs/example.pdf", Zkn3AttachmentKind.FILE, 1);
        assertBlankAttachmentWarning(batch, "1");
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readKeepsLinksLinkSeparateFromManualLinkRecords() throws IOException {
        Path source = createZip(
                "manual-and-attachment-link.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                    <manlinks>2</manlinks>
                                    <links><link>https://example.org/a</link></links>
                                  </zettel>
                                  <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                                    <title>Second</title>
                                    <content>Second body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(1, batch.links().size());
        assertManualLinkRecord(batch.links().get(0), "1", "2");
        assertEquals(1, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "1", "https://example.org/a", Zkn3AttachmentKind.URL, 0);
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readRejectsUnrelatedErrorWithNoAttachmentRecords() throws IOException {
        Path source = createZip(
                "attachment-with-invalid-keyword.zkn3",
                validZknFileEntryWithKeywordsAndLinks("2", "<links><link>https://example.org/a</link></links>"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertRejectedBatchHasNoRecords(batch);
        assertImportRejectedBatchDiagnostic(batch, source);
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywords",
                "Keyword index 2 is out of range for keywordFile.xml with 1 entries."
        );
    }

    @Test
    void readMapsOneOfTwoNotesAttachmentMetadata() throws IOException {
        Path source = createZip(
                "one-of-two-notes-with-attachment.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                                    <title>First</title>
                                    <content>First body</content>
                                    <keywords>1</keywords>
                                  </zettel>
                                  <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                                    <title>Second</title>
                                    <content>Second body</content>
                                    <keywords>1</keywords>
                                    <links><link>/tmp/example.pdf</link></links>
                                  </zettel>
                                </zettelkasten>
                                """
                ),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(2, batch.notes().size());
        assertEquals(2, batch.keywords().size());
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(1, batch.attachments().size());
        assertAttachmentRecord(batch.attachments().get(0), "2", "/tmp/example.pdf", Zkn3AttachmentKind.FILE, 0);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 2);
        assertNoErrorDiagnostics(batch);
    }

    @Test
    void readDeduplicatesDuplicateKeywordReferenceForSameNote() throws IOException {
        Path source = createZip(
                "duplicate-keyword-reference.zkn3",
                validZknFileEntryWithKeywords("1,1"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 1);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readDeduplicatesDuplicateKeywordTextFromDifferentEntriesForSameNote() throws IOException {
        Path source = createZip(
                "duplicate-keyword-text.zkn3",
                validZknFileEntryWithKeywords("1,2"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEquals(1, batch.notes().size());
        assertEquals(1, batch.keywords().size());
        assertKeywordRecord(batch.keywords().get(0), "1", "alpha");
        assertNoLinkOrSequenceRecords(batch);
        assertEquals(5, batch.diagnostics().size());
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 2);
        assertKeywordResolutionDiagnostic(batch, source, 1, 1);
    }

    @Test
    void readRejectsBatchWhenKeywordReferenceTokenIsNotInteger() throws IOException {
        Path source = createZip(
                "keyword-token-not-integer.zkn3",
                validZknFileEntryWithKeywords("abc"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywords",
                "Invalid keyword index token 'abc'; expected one-based integer reference into keywordFile.xml."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordReferenceIsZero() throws IOException {
        Path source = createZip(
                "keyword-token-zero.zkn3",
                validZknFileEntryWithKeywords("0"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywords",
                "Invalid keyword index 0; keyword indexes are one-based and must be greater than zero."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordReferenceIsNegative() throws IOException {
        Path source = createZip(
                "keyword-token-negative.zkn3",
                validZknFileEntryWithKeywords("-1"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywords",
                "Invalid keyword index -1; keyword indexes are one-based and must be greater than zero."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordReferenceIsOutOfRange() throws IOException {
        Path source = createZip(
                "keyword-token-out-of-range.zkn3",
                validZknFileEntryWithKeywords("2"),
                zipEntry("keywordFile.xml", "<keywords><entry>alpha</entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywords",
                "Keyword index 2 is out of range for keywordFile.xml with 1 entries."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordReferenceResolvesToBlankEntry() throws IOException {
        Path source = createZip(
                "keyword-reference-blank-entry.zkn3",
                validZknFileEntryWithKeywords("1"),
                zipEntry("keywordFile.xml", "<keywords><entry>   </entry></keywords>")
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertNoRelationRecords(batch);
        assertEquals(2, batch.diagnostics().size());
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                "1",
                "keywordFile.xml",
                "Keyword index 1 resolves to a blank keyword entry."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordFileRootIsWrong() throws IOException {
        Path source = createZip(
                "wrong-keyword-root.zkn3",
                validZknFileEntry(),
                zipEntry("keywordFile.xml", "<notKeywords/>")
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
                "Expected keywordFile.xml root element keywords but found notKeywords."
        );
        assertImportRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenKeywordFileHasUnexpectedElementChild() throws IOException {
        Path source = createZip(
                "unexpected-keyword-child.zkn3",
                validZknFileEntry(),
                zipEntry("keywordFile.xml", "<keywords><notEntry>foo</notEntry></keywords>")
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
                "Expected keywordFile.xml child element entry but found notEntry."
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
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
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
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
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
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
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
        assertTimestampDiagnostic(batch, "102", "ts_created", "bad");
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readRejectsBatchWhenOneZettelHasBlankCreatedTimestamp() throws IOException {
        Path source = createZip(
                "blank-created.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="103" ts_created="" ts_edited="1700000100" rating="1">
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
        assertTimestampDiagnostic(batch, "103", "ts_created", "");
        assertRejectedBatchDiagnostic(batch, source);
    }

    @Test
    void readAcceptsBlankEditedTimestampByUsingCreatedTimestamp() throws IOException {
        Path source = createZip(
                "blank-edited.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="103" ts_created="1700000000" ts_edited="" rating="1">
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
        Zkn3NoteRecord note = batch.notes().get(0);
        assertEquals("103", note.sourceId());
        assertEquals("1700000000", note.rawCreatedTimestamp());
        assertEquals("", note.rawEditedTimestamp());
        assertEquals(Instant.ofEpochSecond(1700000000L), note.createdAt());
        assertEquals(note.createdAt(), note.modifiedAt());
        assertNoRelationRecords(batch);
        assertNoErrorDiagnostics(batch);
        assertBlankEditedTimestampWarning(batch, "103");
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
    }

    @Test
    void readAcceptsMissingEditedTimestampAsBlank() throws IOException {
        Path source = createZip(
                "missing-edited.zkn3",
                zipEntry(
                        "zknFile.xml",
                        """
                                <zettelkasten>
                                  <zettel zknid="105" ts_created="1700000000" rating="1">
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
        Zkn3NoteRecord note = batch.notes().get(0);
        assertEquals("105", note.sourceId());
        assertEquals("1700000000", note.rawCreatedTimestamp());
        assertEquals("", note.rawEditedTimestamp());
        assertEquals(Instant.ofEpochSecond(1700000000L), note.createdAt());
        assertEquals(note.createdAt(), note.modifiedAt());
        assertNoRelationRecords(batch);
        assertNoErrorDiagnostics(batch);
        assertBlankEditedTimestampWarning(batch, "105");
        assertSummaryDiagnostic(batch, source, 1);
        assertKeywordFileShapeDiagnostic(batch, source, 0);
        assertKeywordResolutionDiagnostic(batch, source, 0, 1);
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
                                  <zettel zknid="104" ts_created="1700000000" ts_edited="not-a-timestamp" rating="1">
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
        assertTimestampDiagnostic(batch, "104", "ts_edited", "not-a-timestamp");
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

    private static ZipFixtureEntry validZknFileEntry() {
        return zipEntry(
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
    }

    private static ZipFixtureEntry validZknFileEntryWithKeywords(String keywords) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>%s</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(keywords)
        );
    }

    private static ZipFixtureEntry validZknFileEntryWithKeywordsAndManlinks(String keywords, String manlinksElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>%s</keywords>
                            %s
                          </zettel>
                        </zettelkasten>
                        """.formatted(keywords, manlinksElement)
        );
    }

    private static ZipFixtureEntry validZknFileEntryWithKeywordsAndLuhmann(String keywords, String luhmannElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>%s</keywords>
                            %s
                          </zettel>
                        </zettelkasten>
                        """.formatted(keywords, luhmannElement)
        );
    }

    private static ZipFixtureEntry validTwoZknFileEntryWithFirstManlinks(String manlinksElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(manlinksElement)
        );
    }

    private static ZipFixtureEntry validTwoZknFileEntryWithFirstLuhmann(String luhmannElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(luhmannElement)
        );
    }

    private static ZipFixtureEntry validTwoZknFileEntryWithFirstManlinksAndLuhmann(
            String manlinksElement,
            String luhmannElement
    ) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(manlinksElement, luhmannElement)
        );
    }

    private static ZipFixtureEntry validThreeZknFileEntryWithFirstManlinks(String manlinksElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                          </zettel>
                          <zettel zknid="3" ts_created="1700000002" ts_edited="1700000102" rating="">
                            <title>Third</title>
                            <content>Third body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(manlinksElement)
        );
    }

    private static ZipFixtureEntry validThreeZknFileEntryWithFirstLuhmann(String luhmannElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                          </zettel>
                          <zettel zknid="3" ts_created="1700000002" ts_edited="1700000102" rating="">
                            <title>Third</title>
                            <content>Third body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(luhmannElement)
        );
    }

    private static ZipFixtureEntry validThreeZknFileEntryWithFirstAndSecondLuhmann(
            String firstLuhmannElement,
            String secondLuhmannElement
    ) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="3" ts_created="1700000002" ts_edited="1700000102" rating="">
                            <title>Third</title>
                            <content>Third body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(firstLuhmannElement, secondLuhmannElement)
        );
    }

    private static ZipFixtureEntry validThreeZknFileEntryWithFirstAndSecondManlinks(
            String firstManlinksElement,
            String secondManlinksElement
    ) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="2" ts_created="1700000001" ts_edited="1700000101" rating="">
                            <title>Second</title>
                            <content>Second body</content>
                            <keywords>1</keywords>
                            %s
                          </zettel>
                          <zettel zknid="3" ts_created="1700000002" ts_edited="1700000102" rating="">
                            <title>Third</title>
                            <content>Third body</content>
                            <keywords>1</keywords>
                          </zettel>
                        </zettelkasten>
                        """.formatted(firstManlinksElement, secondManlinksElement)
        );
    }

    private static ZipFixtureEntry validZknFileEntryWithKeywordsAndLinks(String keywords, String linksElement) {
        return zipEntry(
                "zknFile.xml",
                """
                        <zettelkasten>
                          <zettel zknid="1" ts_created="1700000000" ts_edited="1700000100" rating="">
                            <title>First</title>
                            <content>First body</content>
                            <keywords>%s</keywords>
                            %s
                          </zettel>
                        </zettelkasten>
                        """.formatted(keywords, linksElement)
        );
    }

    private static void assertNoRelationRecords(Zkn3ImportBatch batch) {
        assertEquals(0, batch.keywords().size());
        assertNoLinkOrSequenceRecords(batch);
    }

    private static void assertNoInternalLinkOrSequenceRecords(Zkn3ImportBatch batch) {
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
    }

    private static void assertNoLinkOrSequenceRecords(Zkn3ImportBatch batch) {
        assertNoInternalLinkOrSequenceRecords(batch);
        assertEquals(0, batch.attachments().size());
    }

    private static void assertRejectedBatchHasNoRecords(Zkn3ImportBatch batch) {
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(0, batch.attachments().size());
    }

    private static void assertNoErrorDiagnostics(Zkn3ImportBatch batch) {
        assertTrue(
                batch.diagnostics().stream()
                        .noneMatch(diagnostic -> Zkn3DiagnosticSeverity.ERROR == diagnostic.severity()),
                "Expected no ERROR diagnostics in " + batch.diagnostics()
        );
    }

    private static void assertKeywordRecord(Zkn3KeywordRecord record, String noteSourceId, String keyword) {
        assertEquals(noteSourceId, record.noteSourceId());
        assertEquals(keyword, record.keyword());
    }

    private static void assertManualLinkRecord(Zkn3LinkRecord record, String fromSourceId, String toSourceId) {
        assertEquals(fromSourceId, record.fromSourceId());
        assertEquals(toSourceId, record.toSourceId());
        assertEquals(Zkn3LinkKind.MANUAL, record.kind());
    }

    private static void assertSequenceRecord(
            Zkn3SequenceRecord record,
            String parentSourceId,
            String childSourceId,
            int order
    ) {
        assertEquals(parentSourceId, record.parentSourceId());
        assertEquals(childSourceId, record.childSourceId());
        assertEquals(order, record.order());
    }

    private static void assertAttachmentRecord(
            Zkn3AttachmentRecord record,
            String sourceNoteId,
            String rawValue,
            Zkn3AttachmentKind kind,
            int order
    ) {
        assertEquals(sourceNoteId, record.sourceNoteId());
        assertEquals(rawValue, record.rawValue());
        assertEquals(kind, record.kind());
        assertEquals(order, record.order());
    }

    private static void assertSummaryDiagnostic(Zkn3ImportBatch batch, Path source, int noteCount) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "zettel",
                "Extracted "
                        + noteCount
                        + " ZKN3 note records."
        );
    }

    private static void assertKeywordFileShapeDiagnostic(Zkn3ImportBatch batch, Path source, int entryCount) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "keywordFile.xml",
                "Validated keywordFile.xml root keywords with "
                        + entryCount
                        + " entry elements."
        );
    }

    private static void assertKeywordResolutionDiagnostic(
            Zkn3ImportBatch batch,
            Path source,
            int keywordRecordCount,
            int noteCount
    ) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "keywords",
                "Extracted "
                        + keywordRecordCount
                        + " ZKN3 keyword records for "
                        + noteCount
                        + " notes."
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

    private static void assertInvalidManualLinkIndexDiagnostic(
            Zkn3ImportBatch batch,
            String sourceId,
            int index
    ) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                sourceId,
                "manlinks",
                "Invalid manual link index "
                        + index
                        + "; zettel entry positions are one-based and must be greater than zero."
        );
    }

    private static void assertInvalidLuhmannSequenceIndexDiagnostic(
            Zkn3ImportBatch batch,
            String sourceId,
            int index
    ) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                sourceId,
                "luhmann",
                "Invalid Luhmann sequence index "
                        + index
                        + "; zettel entry positions are one-based and must be greater than zero."
        );
    }

    private static void assertBlankAttachmentWarning(Zkn3ImportBatch batch, String sourceId) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                sourceId,
                "links/link",
                "Blank attachment value for source note '"
                        + sourceId
                        + "'; ignoring blank links/link entry."
        );
    }

    private static void assertManualLinkRecordDiagnostic(
            Zkn3ImportBatch batch,
            Path source,
            int linkRecordCount,
            int noteCount
    ) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "manlinks",
                "Extracted "
                        + linkRecordCount
                        + " ZKN3 manual link records for "
                        + noteCount
                        + " notes."
        );
    }

    private static void assertLuhmannSequenceRecordDiagnostic(
            Zkn3ImportBatch batch,
            Path source,
            int sequenceRecordCount,
            int parentNoteCount
    ) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source.toString(),
                "luhmann",
                "Extracted "
                        + sequenceRecordCount
                        + " ZKN3 Luhmann sequence records for "
                        + parentNoteCount
                        + " parent notes."
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

    private static void assertTimestampDiagnostic(
            Zkn3ImportBatch batch,
            String sourceId,
            String field,
            String rawValue
    ) {
        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().stream()
                .filter(candidate -> Zkn3DiagnosticSeverity.ERROR == candidate.severity()
                        && sourceId.equals(candidate.sourceId())
                        && field.equals(candidate.field()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected timestamp diagnostic for "
                                + sourceId
                                + " "
                                + field
                                + " in "
                                + batch.diagnostics()));

        assertTrue(
                diagnostic.message().contains("source note '" + sourceId + "'"),
                "Expected timestamp diagnostic message to include source note id: " + diagnostic.message()
        );
        assertTrue(
                diagnostic.message().contains("raw " + field + "='" + rawValue + "'"),
                "Expected timestamp diagnostic message to include raw timestamp value: " + diagnostic.message()
        );
    }

    private static void assertBlankEditedTimestampWarning(Zkn3ImportBatch batch, String sourceId) {
        assertDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.WARNING,
                sourceId,
                "ts_edited",
                "Blank edited timestamp for source note '"
                        + sourceId
                        + "'; preserving raw ts_edited='' and using created timestamp as modified timestamp."
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
        assertEquals(0, batch.attachments().size());
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
        assertEquals(0, batch.attachments().size());
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
