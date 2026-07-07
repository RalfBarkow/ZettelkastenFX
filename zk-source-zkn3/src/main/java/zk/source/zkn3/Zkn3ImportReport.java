package zk.source.zkn3;

import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3UnresolvedReferenceKind;
import zk.core.importing.Zkn3UnresolvedReferenceReason;
import zk.core.importing.Zkn3UnresolvedReferenceRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Non-UI import report projection over a ZKN3 import batch.
 *
 * <p>This report summarizes unresolved source references as first-class import
 * outcomes. It does not resolve targets, repair source data, or create graph
 * edges.</p>
 */
public record Zkn3ImportReport(
        int noteCount,
        int keywordCount,
        int linkCount,
        int sequenceCount,
        int attachmentCount,
        int diagnosticCount,
        int unresolvedReferenceCount,
        List<UnresolvedReferenceKindCount> unresolvedReferencesByKind,
        List<UnresolvedReferenceReasonCount> unresolvedReferencesByReason,
        List<Zkn3UnresolvedReferenceRecord> unresolvedReferenceExamples
) {
    public static final int DEFAULT_UNRESOLVED_REFERENCE_EXAMPLE_LIMIT = 5;

    public Zkn3ImportReport {
        unresolvedReferencesByKind = List.copyOf(unresolvedReferencesByKind);
        unresolvedReferencesByReason = List.copyOf(unresolvedReferencesByReason);
        unresolvedReferenceExamples = List.copyOf(unresolvedReferenceExamples);
    }

    public static Zkn3ImportReport from(Zkn3ImportBatch batch) {
        return from(batch, DEFAULT_UNRESOLVED_REFERENCE_EXAMPLE_LIMIT);
    }

    public static Zkn3ImportReport from(Zkn3ImportBatch batch, int unresolvedReferenceExampleLimit) {
        Objects.requireNonNull(batch, "batch");
        if (unresolvedReferenceExampleLimit < 0) {
            throw new IllegalArgumentException("unresolvedReferenceExampleLimit must be non-negative");
        }

        return new Zkn3ImportReport(
                batch.notes().size(),
                batch.keywords().size(),
                batch.links().size(),
                batch.sequences().size(),
                batch.attachments().size(),
                batch.diagnostics().size(),
                batch.unresolvedReferences().size(),
                unresolvedReferencesByKind(batch.unresolvedReferences()),
                unresolvedReferencesByReason(batch.unresolvedReferences()),
                batch.unresolvedReferences().stream()
                        .limit(unresolvedReferenceExampleLimit)
                        .toList()
        );
    }

    public boolean hasUnresolvedReferences() {
        return unresolvedReferenceCount > 0;
    }

    private static List<UnresolvedReferenceKindCount> unresolvedReferencesByKind(
            List<Zkn3UnresolvedReferenceRecord> unresolvedReferences
    ) {
        Map<Zkn3UnresolvedReferenceKind, Long> counts = new LinkedHashMap<>();
        for (Zkn3UnresolvedReferenceRecord reference : unresolvedReferences) {
            counts.merge(reference.referenceKind(), 1L, Long::sum);
        }

        List<UnresolvedReferenceKindCount> result = new ArrayList<>();
        counts.forEach((kind, count) -> result.add(new UnresolvedReferenceKindCount(kind, count)));
        return result;
    }

    private static List<UnresolvedReferenceReasonCount> unresolvedReferencesByReason(
            List<Zkn3UnresolvedReferenceRecord> unresolvedReferences
    ) {
        Map<Zkn3UnresolvedReferenceReason, Long> counts = new LinkedHashMap<>();
        for (Zkn3UnresolvedReferenceRecord reference : unresolvedReferences) {
            counts.merge(reference.reason(), 1L, Long::sum);
        }

        List<UnresolvedReferenceReasonCount> result = new ArrayList<>();
        counts.forEach((reason, count) -> result.add(new UnresolvedReferenceReasonCount(reason, count)));
        return result;
    }

    public record UnresolvedReferenceKindCount(
            Zkn3UnresolvedReferenceKind kind,
            long count
    ) {
    }

    public record UnresolvedReferenceReasonCount(
            Zkn3UnresolvedReferenceReason reason,
            long count
    ) {
    }
}
