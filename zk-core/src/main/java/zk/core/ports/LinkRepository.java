package zk.core.ports;

import zk.core.model.NoteId;

import java.util.stream.Stream;

public interface LinkRepository {
    Stream<NoteId> outgoing(NoteId from);
    Stream<NoteId> incoming(NoteId to);
    void add(NoteId from, NoteId to);
    void remove(NoteId from, NoteId to);
}
