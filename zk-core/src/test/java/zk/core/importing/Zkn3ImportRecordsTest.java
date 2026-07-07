package zk.core.importing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3ImportRecordsTest {

    @Test
    void recordsComposeIntoImportBatch() {
        Instant createdAt = Instant.parse("2026-07-05T09:00:00Z");
        Instant modifiedAt = Instant.parse("2026-07-05T09:30:00Z");
        Zkn3NoteRecord note = new Zkn3NoteRecord(
                "09080814571Zettelkasten1",
                "Title",
                "Body",
                "0908081457",
                "0908081512",
                createdAt,
                modifiedAt,
                OptionalInt.of(3)
        );
        Zkn3KeywordRecord keyword = new Zkn3KeywordRecord(note.sourceId(), "Zettelkasten");
        Zkn3LinkRecord normalLink = new Zkn3LinkRecord(note.sourceId(), "target-normal", Zkn3LinkKind.NORMAL);
        Zkn3LinkRecord manualLink = new Zkn3LinkRecord(note.sourceId(), "target-manual", Zkn3LinkKind.MANUAL);
        Zkn3SequenceRecord sequence = new Zkn3SequenceRecord(note.sourceId(), "child", 0);
        Zkn3AttachmentRecord firstAttachment = new Zkn3AttachmentRecord(
                note.sourceId(),
                "http://example.test/document",
                Zkn3AttachmentKind.URL,
                0
        );
        Zkn3AttachmentRecord secondAttachment = new Zkn3AttachmentRecord(
                note.sourceId(),
                "files/document.pdf",
                Zkn3AttachmentKind.FILE,
                1
        );
        Zkn3ImportDiagnostic diagnostic = new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.WARNING,
                note.sourceId(),
                "ts_edited",
                "Missing modified timestamp"
        );
        List<Zkn3AttachmentRecord> attachments = new ArrayList<>(List.of(firstAttachment, secondAttachment));

        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(note),
                List.of(keyword),
                List.of(normalLink, manualLink),
                List.of(sequence),
                attachments,
                List.of(diagnostic)
        );
        attachments.clear();

        assertEquals(note, batch.notes().get(0));
        assertEquals("0908081457", note.rawCreatedTimestamp());
        assertEquals("0908081512", note.rawEditedTimestamp());
        assertEquals(createdAt, note.createdAt());
        assertEquals(modifiedAt, note.modifiedAt());
        assertEquals(keyword, batch.keywords().get(0));
        assertEquals(Zkn3LinkKind.NORMAL, batch.links().get(0).kind());
        assertEquals(Zkn3LinkKind.MANUAL, batch.links().get(1).kind());
        assertEquals(sequence, batch.sequences().get(0));
        assertEquals(firstAttachment, batch.attachments().get(0));
        assertEquals(secondAttachment, batch.attachments().get(1));
        assertEquals(Zkn3DiagnosticSeverity.WARNING, batch.diagnostics().get(0).severity());
        assertThrows(UnsupportedOperationException.class, () -> batch.notes().add(note));
        assertThrows(UnsupportedOperationException.class, () -> batch.attachments().add(firstAttachment));
    }

    @Test
    void attachmentRecordPreservesFields() {
        Zkn3AttachmentRecord attachment = new Zkn3AttachmentRecord(
                "09080814571Zettelkasten1",
                "  relative path/document.pdf  ",
                Zkn3AttachmentKind.UNKNOWN,
                3
        );

        assertEquals("09080814571Zettelkasten1", attachment.sourceNoteId());
        assertEquals("  relative path/document.pdf  ", attachment.rawValue());
        assertEquals(Zkn3AttachmentKind.UNKNOWN, attachment.kind());
        assertEquals(3, attachment.order());
    }

    @Test
    void attachmentRecordRejectsInvalidConstructorArguments() {
        assertThrows(NullPointerException.class, () -> new Zkn3AttachmentRecord(
                null,
                "file.pdf",
                Zkn3AttachmentKind.FILE,
                0
        ));
        assertThrows(IllegalArgumentException.class, () -> new Zkn3AttachmentRecord(
                " ",
                "file.pdf",
                Zkn3AttachmentKind.FILE,
                0
        ));
        assertThrows(NullPointerException.class, () -> new Zkn3AttachmentRecord(
                "09080814571Zettelkasten1",
                null,
                Zkn3AttachmentKind.FILE,
                0
        ));
        assertThrows(IllegalArgumentException.class, () -> new Zkn3AttachmentRecord(
                "09080814571Zettelkasten1",
                " ",
                Zkn3AttachmentKind.FILE,
                0
        ));
        assertThrows(NullPointerException.class, () -> new Zkn3AttachmentRecord(
                "09080814571Zettelkasten1",
                "file.pdf",
                null,
                0
        ));
        assertThrows(IllegalArgumentException.class, () -> new Zkn3AttachmentRecord(
                "09080814571Zettelkasten1",
                "file.pdf",
                Zkn3AttachmentKind.FILE,
                -1
        ));
    }

    @Test
    void emptyBatchHasEmptyAttachments() {
        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        assertTrue(batch.attachments().isEmpty());
    }

    @Test
    void noteRecordRequiresRawTimestampValues() {
        Instant createdAt = Instant.parse("2026-07-05T09:00:00Z");
        Instant modifiedAt = Instant.parse("2026-07-05T09:30:00Z");

        assertThrows(NullPointerException.class, () -> new Zkn3NoteRecord(
                "09080814571Zettelkasten1",
                "Title",
                "Body",
                null,
                "0908081512",
                createdAt,
                modifiedAt,
                OptionalInt.empty()
        ));
        assertThrows(NullPointerException.class, () -> new Zkn3NoteRecord(
                "09080814571Zettelkasten1",
                "Title",
                "Body",
                "0908081457",
                null,
                createdAt,
                modifiedAt,
                OptionalInt.empty()
        ));
        assertThrows(IllegalArgumentException.class, () -> new Zkn3NoteRecord(
                "09080814571Zettelkasten1",
                "Title",
                "Body",
                "",
                "0908081512",
                createdAt,
                modifiedAt,
                OptionalInt.empty()
        ));
    }
}
