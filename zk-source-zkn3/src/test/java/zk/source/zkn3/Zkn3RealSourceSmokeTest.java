package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

final class Zkn3RealSourceSmokeTest {
    private static final String REAL_SOURCE_PROPERTY = "zkn3.real.source";
    private static final int FIRST_ERROR_LIMIT = 5;

    @Test
    void readsRealSourceOnlyWhenOptedIn() throws IOException {
        String propertyValue = System.getProperty(REAL_SOURCE_PROPERTY, "").trim();
        assumeFalse(propertyValue.isEmpty(), REAL_SOURCE_PROPERTY + " is not set; skipping real-source smoke.");

        Path source = Path.of(propertyValue);
        assertTrue(Files.isRegularFile(source), "Expected readable real ZKN3 source at " + source);
        assertTrue(Files.isReadable(source), "Expected readable real ZKN3 source at " + source);

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertNotNull(batch.diagnostics());

        long errorCount = countDiagnostics(batch, Zkn3DiagnosticSeverity.ERROR);
        long warningCount = countDiagnostics(batch, Zkn3DiagnosticSeverity.WARNING);
        String status = errorCount == 0 ? "IMPORTABLE" : "CORRECTLY_REJECTED";
        List<String> firstErrors = firstErrors(batch);

        printSummary(source, batch, status, errorCount, warningCount, firstErrors);

        if (errorCount > 0) {
            assertEquals(0, batch.notes().size(), "Rejected real-source batch must not contain notes.");
            assertEquals(0, batch.keywords().size(), "Rejected real-source batch must not contain keywords.");
            assertEquals(0, batch.links().size(), "Rejected real-source batch must not contain links.");
            assertEquals(0, batch.sequences().size(), "Rejected real-source batch must not contain sequences.");
        }
    }

    private static long countDiagnostics(Zkn3ImportBatch batch, Zkn3DiagnosticSeverity severity) {
        return batch.diagnostics().stream()
                .filter(diagnostic -> severity == diagnostic.severity())
                .count();
    }

    private static List<String> firstErrors(Zkn3ImportBatch batch) {
        return batch.diagnostics().stream()
                .filter(diagnostic -> Zkn3DiagnosticSeverity.ERROR == diagnostic.severity())
                .limit(FIRST_ERROR_LIMIT)
                .map(Zkn3RealSourceSmokeTest::formatDiagnostic)
                .toList();
    }

    private static String formatDiagnostic(Zkn3ImportDiagnostic diagnostic) {
        return diagnostic.sourceId()
                + " "
                + diagnostic.field()
                + " "
                + diagnostic.message();
    }

    private static void printSummary(
            Path source,
            Zkn3ImportBatch batch,
            String status,
            long errorCount,
            long warningCount,
            List<String> firstErrors
    ) {
        System.out.println("ZKN3 real-source smoke:");
        System.out.println("source=" + source);
        System.out.println("status=" + status);
        System.out.println("notes=" + batch.notes().size());
        System.out.println("keywords=" + batch.keywords().size());
        System.out.println("links=" + batch.links().size());
        System.out.println("sequences=" + batch.sequences().size());
        System.out.println("diagnostics=" + batch.diagnostics().size());
        System.out.println("errors=" + errorCount);
        System.out.println("warnings=" + warningCount);
        System.out.println("firstErrors=" + firstErrors);
    }
}
