package zk.core.ports;

import zk.core.model.NoteDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface Zkn3SourceReader {
    Stream<NoteDTO> readNotes(Path zkn3File) throws IOException;
}
