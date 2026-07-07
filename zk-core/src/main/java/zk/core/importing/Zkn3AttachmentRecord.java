package zk.core.importing;

import java.util.Objects;

public record Zkn3AttachmentRecord(
        String sourceNoteId,
        String rawValue,
        Zkn3AttachmentKind kind,
        int order
) {
    public Zkn3AttachmentRecord {
        Objects.requireNonNull(sourceNoteId, "sourceNoteId");
        Objects.requireNonNull(rawValue, "rawValue");
        Objects.requireNonNull(kind, "kind");
        if (sourceNoteId.isBlank()) {
            throw new IllegalArgumentException("sourceNoteId must not be blank");
        }
        if (rawValue.isBlank()) {
            throw new IllegalArgumentException("rawValue must not be blank");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must be zero or greater");
        }
    }
}
