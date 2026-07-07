package zk.core.importing;

import java.util.List;

public record Zkn3ImportBatch(
        List<Zkn3NoteRecord> notes,
        List<Zkn3KeywordRecord> keywords,
        List<Zkn3LinkRecord> links,
        List<Zkn3SequenceRecord> sequences,
        List<Zkn3AttachmentRecord> attachments,
        List<Zkn3ImportDiagnostic> diagnostics
,
        List<Zkn3UnresolvedReferenceRecord> unresolvedReferences) {
    /**
     * Compatibility constructor preserving the pre-unresolvedReferences batch shape.
     */
    public Zkn3ImportBatch(
        List<Zkn3NoteRecord> notes,
        List<Zkn3KeywordRecord> keywords,
        List<Zkn3LinkRecord> links,
        List<Zkn3SequenceRecord> sequences,
        List<Zkn3AttachmentRecord> attachments,
        List<Zkn3ImportDiagnostic> diagnostics
    ) {
        this(notes, keywords, links, sequences, attachments, diagnostics, List.of());
    }


    public Zkn3ImportBatch {
        unresolvedReferences = List.copyOf(unresolvedReferences);
        notes = List.copyOf(notes);
        keywords = List.copyOf(keywords);
        links = List.copyOf(links);
        sequences = List.copyOf(sequences);
        attachments = List.copyOf(attachments);
        diagnostics = List.copyOf(diagnostics);
    }
}
