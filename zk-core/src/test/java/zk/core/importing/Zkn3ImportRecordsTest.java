package zk.core.importing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class Zkn3ImportRecordsTest {

    @Test
    void recordsComposeIntoImportBatch() {
        Instant createdAt = Instant.parse("2026-07-05T09:00:00Z");
        Instant modifiedAt = Instant.parse("2026-07-05T09:30:00Z");
        Zkn3NoteRecord note = new Zkn3NoteRecord(
                "09080814571Zettelkasten1",
                "Title",
                "Body",
                createdAt,
                modifiedAt,
                OptionalInt.of(3)
        );
        Zkn3KeywordRecord keyword = new Zkn3KeywordRecord(note.sourceId(), "Zettelkasten");
        Zkn3LinkRecord normalLink = new Zkn3LinkRecord(note.sourceId(), "target-normal", Zkn3LinkKind.NORMAL);
        Zkn3LinkRecord manualLink = new Zkn3LinkRecord(note.sourceId(), "target-manual", Zkn3LinkKind.MANUAL);
        Zkn3SequenceRecord sequence = new Zkn3SequenceRecord(note.sourceId(), "child", 0);
        Zkn3ImportDiagnostic diagnostic = new Zkn3ImportDiagnostic(
                Zkn3DiagnosticSeverity.WARNING,
                note.sourceId(),
                "ts_edited",
                "Missing modified timestamp"
        );

        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(note),
                List.of(keyword),
                List.of(normalLink, manualLink),
                List.of(sequence),
                List.of(diagnostic)
        );

        assertEquals(note, batch.notes().get(0));
        assertEquals(keyword, batch.keywords().get(0));
        assertEquals(Zkn3LinkKind.NORMAL, batch.links().get(0).kind());
        assertEquals(Zkn3LinkKind.MANUAL, batch.links().get(1).kind());
        assertEquals(sequence, batch.sequences().get(0));
        assertEquals(Zkn3DiagnosticSeverity.WARNING, batch.diagnostics().get(0).severity());
        assertThrows(UnsupportedOperationException.class, () -> batch.notes().add(note));
    }
}
