package zk.core.importing;

public record Zkn3ImportDiagnostic(
        Zkn3DiagnosticSeverity severity,
        String sourceId,
        String field,
        String message
) {}
