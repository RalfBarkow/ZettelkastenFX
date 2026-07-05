package zk.core.importing;

public record Zkn3LinkRecord(
        String fromSourceId,
        String toSourceId,
        Zkn3LinkKind kind
) {}
