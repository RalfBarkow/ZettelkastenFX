package zk.source.zkn3;

import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.ports.Zkn3SourceReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class Zkn3DomSourceReader implements Zkn3SourceReader {
    @Override
    public Zkn3ImportBatch read(Path zkn3File) throws IOException {
        Objects.requireNonNull(zkn3File, "zkn3File");

        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new Zkn3ImportDiagnostic(
                        Zkn3DiagnosticSeverity.INFO,
                        zkn3File.toString(),
                        "reader",
                        "ZKN3 DOM reader skeleton: parsing not implemented yet."
                ))
        );
    }
}
