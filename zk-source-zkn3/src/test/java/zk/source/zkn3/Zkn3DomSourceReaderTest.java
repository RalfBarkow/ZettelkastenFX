package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.ports.Zkn3SourceReader;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class Zkn3DomSourceReaderTest {

    @Test
    void implementsSourceReaderPort() {
        assertInstanceOf(Zkn3SourceReader.class, new Zkn3DomSourceReader());
    }

    @Test
    void readReturnsEmptyBatchWithNotImplementedDiagnostic() throws IOException {
        Path source = Path.of("example.zkn3");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(1, batch.diagnostics().size());

        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().get(0);
        assertEquals(Zkn3DiagnosticSeverity.INFO, diagnostic.severity());
        assertEquals(source.toString(), diagnostic.sourceId());
        assertEquals("reader", diagnostic.field());
        assertEquals("ZKN3 DOM reader skeleton: parsing not implemented yet.", diagnostic.message());
    }

    @Test
    void readRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new Zkn3DomSourceReader().read(null));
    }
}
