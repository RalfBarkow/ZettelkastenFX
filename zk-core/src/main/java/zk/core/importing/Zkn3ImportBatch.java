package zk.core.importing;

import java.util.List;

public record Zkn3ImportBatch(
        List<Zkn3NoteRecord> notes,
        List<Zkn3KeywordRecord> keywords,
        List<Zkn3LinkRecord> links,
        List<Zkn3SequenceRecord> sequences,
        List<Zkn3ImportDiagnostic> diagnostics
) {
    public Zkn3ImportBatch {
        notes = List.copyOf(notes);
        keywords = List.copyOf(keywords);
        links = List.copyOf(links);
        sequences = List.copyOf(sequences);
        diagnostics = List.copyOf(diagnostics);
    }
}
