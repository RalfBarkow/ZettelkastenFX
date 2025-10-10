package zk.core.ports;

import zk.core.model.NoteId;

import java.util.Set;
import java.util.stream.Stream;

public interface KeywordRepository {
    Set<String> getFor(NoteId id);
    void add(NoteId id, String keyword);
    void remove(NoteId id, String keyword);
    Stream<String> all();
}
