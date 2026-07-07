package zk.core.importing;

/**
 * Preserves a syntactically valid source reference that could not be resolved
 * to an imported model target.
 *
 * <p>This is deliberately distinct from {@code Zkn3LinkRecord}. A
 * {@code Zkn3LinkRecord} represents a resolved source-note to target-note edge.
 * This record preserves source data without inventing a graph edge.</p>
 */
public record Zkn3UnresolvedReferenceRecord(
        String sourceNoteId,
        String sourceField,
        String rawReference,
        Zkn3UnresolvedReferenceKind referenceKind,
        Zkn3UnresolvedReferenceReason reason,
        int order
) {
    public Zkn3UnresolvedReferenceRecord {
        if (sourceNoteId == null || sourceNoteId.isBlank()) {
            throw new IllegalArgumentException("sourceNoteId must not be blank");
        }
        if (sourceField == null || sourceField.isBlank()) {
            throw new IllegalArgumentException("sourceField must not be blank");
        }
        if (rawReference == null) {
            throw new IllegalArgumentException("rawReference must not be null");
        }
        if (referenceKind == null) {
            throw new IllegalArgumentException("referenceKind must not be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must not be negative");
        }
    }
}
