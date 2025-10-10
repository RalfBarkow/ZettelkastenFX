package zk.core.model;

import java.time.Instant;

public record NoteDTO(
        NoteId id,
        String title,
        String body,
        Instant createdAt,
        Instant modifiedAt,
        int rating
) {}
