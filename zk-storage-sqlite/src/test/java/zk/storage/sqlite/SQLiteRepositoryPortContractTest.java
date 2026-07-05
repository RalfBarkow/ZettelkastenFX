package zk.storage.sqlite;

import org.junit.jupiter.api.Test;
import zk.core.ports.KeywordRepository;
import zk.core.ports.LinkRepository;
import zk.core.ports.NoteRepository;
import zk.core.ports.SequenceRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SQLiteRepositoryPortContractTest {

    @Test
    void sqliteRepositoriesImplementCorePorts() {
        assertTrue(NoteRepository.class.isAssignableFrom(NoteRepositorySQLite.class));
        assertTrue(KeywordRepository.class.isAssignableFrom(KeywordRepositorySQLite.class));
        assertTrue(LinkRepository.class.isAssignableFrom(LinkRepositorySQLite.class));
        assertTrue(SequenceRepository.class.isAssignableFrom(SequenceRepositorySQLite.class));
    }
}
