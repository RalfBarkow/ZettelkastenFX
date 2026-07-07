package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.importing.Zkn3UnresolvedReferenceKind;
import zk.core.importing.Zkn3UnresolvedReferenceReason;
import zk.core.importing.Zkn3UnresolvedReferenceRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3ImportReportTest {
    @Test
    void reportProjectsUnresolvedReferencesWithoutCreatingResolvedEdges() {
        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new Zkn3ImportDiagnostic(
                        Zkn3DiagnosticSeverity.WARNING,
                        "1",
                        "manlinks",
                        "Manual link index 64444 is out of range; preserving unresolved manual link reference."
                )),
                List.of(
                        unresolvedManualLink("1", "64444", 0),
                        unresolvedManualLink("2", "99999", 1)
                )
        );

        Zkn3ImportReport report = Zkn3ImportReport.from(batch, 1);

        assertEquals(0, report.noteCount());
        assertEquals(0, report.keywordCount());
        assertEquals(0, report.linkCount(), "Report projection must not create resolved Zkn3LinkRecord edges.");
        assertEquals(0, report.sequenceCount());
        assertEquals(0, report.attachmentCount());
        assertEquals(1, report.diagnosticCount());
        assertEquals(2, report.unresolvedReferenceCount());
        assertTrue(report.hasUnresolvedReferences());

        assertEquals(
                List.of(new Zkn3ImportReport.UnresolvedReferenceKindCount(
                        Zkn3UnresolvedReferenceKind.MANUAL_LINK,
                        2
                )),
                report.unresolvedReferencesByKind()
        );

        assertEquals(
                List.of(new Zkn3ImportReport.UnresolvedReferenceReasonCount(
                        Zkn3UnresolvedReferenceReason.OUT_OF_RANGE,
                        2
                )),
                report.unresolvedReferencesByReason()
        );

        assertEquals(1, report.unresolvedReferenceExamples().size());
        assertEquals("64444", report.unresolvedReferenceExamples().get(0).rawReference());
    }

    @Test
    void reportHandlesBatchWithoutUnresolvedReferences() {
        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        Zkn3ImportReport report = Zkn3ImportReport.from(batch);

        assertEquals(0, report.unresolvedReferenceCount());
        assertFalse(report.hasUnresolvedReferences());
        assertEquals(List.of(), report.unresolvedReferencesByKind());
        assertEquals(List.of(), report.unresolvedReferencesByReason());
        assertEquals(List.of(), report.unresolvedReferenceExamples());
    }

    @Test
    void reportRejectsNegativeExampleLimit() {
        Zkn3ImportBatch batch = new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> Zkn3ImportReport.from(batch, -1));
    }

    private static Zkn3UnresolvedReferenceRecord unresolvedManualLink(
            String sourceNoteId,
            String rawReference,
            int order
    ) {
        return new Zkn3UnresolvedReferenceRecord(
                sourceNoteId,
                "manlinks",
                rawReference,
                Zkn3UnresolvedReferenceKind.MANUAL_LINK,
                Zkn3UnresolvedReferenceReason.OUT_OF_RANGE,
                order
        );
    }
}
