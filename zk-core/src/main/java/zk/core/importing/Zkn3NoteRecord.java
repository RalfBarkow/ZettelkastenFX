package zk.core.importing;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalInt;

public record Zkn3NoteRecord(
        String sourceId,
        String title,
        String body,
        String rawCreatedTimestamp,
        String rawEditedTimestamp,
        Instant createdAt,
        Instant modifiedAt,
        OptionalInt rating
) {
    public Zkn3NoteRecord {
        Objects.requireNonNull(rawCreatedTimestamp, "rawCreatedTimestamp");
        Objects.requireNonNull(rawEditedTimestamp, "rawEditedTimestamp");
        if (rawCreatedTimestamp.isBlank()) {
            throw new IllegalArgumentException("rawCreatedTimestamp must not be blank");
        }
    }
}
