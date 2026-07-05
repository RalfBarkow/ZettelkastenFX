package zk.core.ports;

import zk.core.importing.Zkn3ImportBatch;

import java.io.IOException;
import java.nio.file.Path;

public interface Zkn3SourceReader {
    Zkn3ImportBatch read(Path zkn3File) throws IOException;
}
