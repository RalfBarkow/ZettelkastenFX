package zk.core.importing;

import java.time.Instant;
import java.util.OptionalInt;

public record Zkn3NoteRecord(
        String sourceId,
        String title,
        String body,
        Instant createdAt,
        Instant modifiedAt,
        OptionalInt rating
) {}
