package zk.core.ports;

import zk.core.model.NoteDTO;
import zk.core.model.NoteId;

import java.util.Optional;
import java.util.stream.Stream;

public interface NoteRepository {
    Optional<NoteDTO> get(NoteId id);
    NoteId create(String title, String body);
    void update(NoteDTO note);
    void delete(NoteId id);
    Stream<NoteDTO> all();
}
