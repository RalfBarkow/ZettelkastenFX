package zk.storage.sqlite;

import org.junit.jupiter.api.Test;
import zk.core.model.NoteDTO;
import zk.core.model.NoteId;
import zk.core.ports.KeywordRepository;
import zk.core.ports.LinkRepository;
import zk.core.ports.NoteRepository;
import zk.core.ports.SequenceRepository;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class SQLiteRepositoriesTest {

    private static DataSource newDb() throws Exception {
        Path dir = Files.createTempDirectory("zk-sqlite-test");
        String url = "jdbc:sqlite:" + dir.resolve("zk.db").toAbsolutePath();
        return new SQLiteDatabase(url).migrateAndGetDataSource();
    }

    @Test void crud_flow() throws Exception {
        DataSource ds = newDb();

        NoteRepository notes = new NoteRepositorySQLite(ds);
        KeywordRepository kw = new KeywordRepositorySQLite(ds);
        LinkRepository links = new LinkRepositorySQLite(ds);
        SequenceRepository seq = new SequenceRepositorySQLite(ds);

        // create notes
        NoteId a = notes.create("A", "Body A");
        NoteId b = notes.create("B", "Body B");

        assertTrue(notes.get(a).isPresent());
        assertEquals("A", notes.get(a).get().title());

        // update rating/title
        NoteDTO na = notes.get(a).get();
        notes.update(new NoteDTO(a, "A*",
                na.body(), na.createdAt(), Instant.now(), 3));
        assertEquals(3, notes.get(a).get().rating());
        assertEquals("A*", notes.get(a).get().title());

        // keywords
        kw.add(a, "foo"); kw.add(a, "bar");
        assertEquals(2, kw.getFor(a).size());
        kw.remove(a, "foo");
        assertEquals(1, kw.getFor(a).size());

        // links
        links.add(a, b);
        assertEquals(1, links.outgoing(a).count());
        assertEquals(1, links.incoming(b).count());
        links.remove(a, b);
        assertEquals(0, links.outgoing(a).count());

        // sequence
        seq.insertChild(a, b, 0);
        assertEquals(b.value(), seq.childrenOf(a).get(0).value());
        assertEquals(a.value(), seq.parentOf(b).get().value());
        seq.detach(b);
        assertTrue(seq.parentOf(b).isEmpty());

        // delete
        notes.delete(a);
        assertFalse(notes.get(a).isPresent());
    }
}
