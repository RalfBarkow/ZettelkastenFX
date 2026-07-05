package zk.source.zkn3;

import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.ports.Zkn3SourceReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Zkn3DomSourceReader implements Zkn3SourceReader {
    private static final String ZKN_FILE_ENTRY = "zknFile.xml";

    @Override
    public Zkn3ImportBatch read(Path zkn3File) throws IOException {
        Objects.requireNonNull(zkn3File, "zkn3File");

        try (ZipFile zipFile = new ZipFile(zkn3File.toFile())) {
            ZipEntry zknFile = zipFile.getEntry(ZKN_FILE_ENTRY);
            if (zknFile != null) {
                return emptyBatchWithDiagnostic(
                        zkn3File,
                        Zkn3DiagnosticSeverity.INFO,
                        "Found zknFile.xml; XML parsing not implemented yet."
                );
            }

            return emptyBatchWithDiagnostic(
                    zkn3File,
                    Zkn3DiagnosticSeverity.ERROR,
                    "Missing required zknFile.xml entry in ZKN3 container."
            );
        }
    }

    private static Zkn3ImportBatch emptyBatchWithDiagnostic(
            Path zkn3File,
            Zkn3DiagnosticSeverity severity,
            String message
    ) {
        return new Zkn3ImportBatch(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new Zkn3ImportDiagnostic(
                        severity,
                        zkn3File.toString(),
                        ZKN_FILE_ENTRY,
                        message
                ))
        );
    }
}
