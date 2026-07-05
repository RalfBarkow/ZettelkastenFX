package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;
import zk.core.ports.Zkn3SourceReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class Zkn3DomSourceReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void implementsSourceReaderPort() {
        assertInstanceOf(Zkn3SourceReader.class, new Zkn3DomSourceReader());
    }

    @Test
    void readReturnsInfoDiagnosticWhenZknFileHasNoZettelElements() throws IOException {
        Path source = createZip("valid-root.zkn3", "zknFile.xml", "<zettelkasten/>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source,
                "zettel",
                "Found zknFile.xml root element zettelkasten with 0 zettel elements; zettel mapping not implemented yet."
        );
    }

    @Test
    void readReturnsInfoDiagnosticWithZettelElementCount() throws IOException {
        Path source = createZip(
                "two-zettel.zkn3",
                "zknFile.xml",
                "<zettelkasten><zettel/><zettel/></zettelkasten>"
        );

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.INFO,
                source,
                "zettel",
                "Found zknFile.xml root element zettelkasten with 2 zettel elements; zettel mapping not implemented yet."
        );
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileRootIsWrong() throws IOException {
        Path source = createZip("wrong-root.zkn3", "zknFile.xml", "<notzettelkasten/>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Expected root element zettelkasten but found notzettelkasten."
        );
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileEntryIsMissing() throws IOException {
        Path source = createZip("missing-zkn-file.zkn3", "other.xml", "<other/>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnostic(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Missing required zknFile.xml entry in ZKN3 container."
        );
    }

    @Test
    void readReturnsErrorDiagnosticWhenZknFileXmlIsMalformed() throws IOException {
        Path source = createZip("malformed-zkn-file.zkn3", "zknFile.xml", "<zettelkasten>");

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);

        assertEmptyBatchWithDiagnosticPrefix(
                batch,
                Zkn3DiagnosticSeverity.ERROR,
                source,
                "zknFile.xml",
                "Could not parse zknFile.xml root element:"
        );
    }

    @Test
    void readRejectsInvalidZip() throws IOException {
        Path source = tempDir.resolve("invalid.zkn3");
        Files.writeString(source, "not a zip");

        assertThrows(IOException.class, () -> new Zkn3DomSourceReader().read(source));
    }

    @Test
    void readRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new Zkn3DomSourceReader().read(null));
    }

    private Path createZip(String fileName, String entryName, String content) throws IOException {
        Path source = tempDir.resolve(fileName);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(source))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return source;
    }

    private static void assertEmptyBatchWithDiagnostic(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            Path source,
            String field,
            String message
    ) {
        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(1, batch.diagnostics().size());

        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().get(0);
        assertEquals(severity, diagnostic.severity());
        assertEquals(source.toString(), diagnostic.sourceId());
        assertEquals(field, diagnostic.field());
        assertEquals(message, diagnostic.message());
    }

    private static void assertEmptyBatchWithDiagnosticPrefix(
            Zkn3ImportBatch batch,
            Zkn3DiagnosticSeverity severity,
            Path source,
            String field,
            String messagePrefix
    ) {
        assertNotNull(batch);
        assertEquals(0, batch.notes().size());
        assertEquals(0, batch.keywords().size());
        assertEquals(0, batch.links().size());
        assertEquals(0, batch.sequences().size());
        assertEquals(1, batch.diagnostics().size());

        Zkn3ImportDiagnostic diagnostic = batch.diagnostics().get(0);
        assertEquals(severity, diagnostic.severity());
        assertEquals(source.toString(), diagnostic.sourceId());
        assertEquals(field, diagnostic.field());
        assertEquals(messagePrefix, diagnostic.message().substring(0, messagePrefix.length()));
    }
}
