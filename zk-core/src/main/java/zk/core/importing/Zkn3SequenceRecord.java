package zk.core.importing;

public record Zkn3SequenceRecord(
        String parentSourceId,
        String childSourceId,
        int order
) {}
