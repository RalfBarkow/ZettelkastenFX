package zk.core.ports;

import zk.core.model.NoteId;

import java.util.List;
import java.util.Optional;

public interface SequenceRepository {
    List<NoteId> childrenOf(NoteId parent);
    Optional<NoteId> parentOf(NoteId child);
    void insertChild(NoteId parent, NoteId child, int order);
    void reorder(NoteId parent, List<NoteId> orderedChildren);
    void detach(NoteId child);
}
