package zk.core.ports;

import org.junit.jupiter.api.Test;
import zk.core.importing.Zkn3ImportBatch;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3SourceReaderPortTest {

    @Test
    void readReturnsImportBatchAndDeclaresIOException() throws NoSuchMethodException {
        Method read = Zkn3SourceReader.class.getMethod("read", Path.class);

        assertEquals(Zkn3ImportBatch.class, read.getReturnType());
        assertTrue(Arrays.asList(read.getExceptionTypes()).contains(IOException.class));
    }
}
