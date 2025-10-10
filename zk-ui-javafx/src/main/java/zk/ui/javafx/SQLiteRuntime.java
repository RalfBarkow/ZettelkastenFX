package zk.ui.javafx;

import zk.core.ports.*;
import zk.storage.sqlite.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;

final class SQLiteRuntime {
    final DataSource ds;
    final NoteRepository notes;
    final KeywordRepository keywords;
    final LinkRepository links;
    final SequenceRepository seq;

    SQLiteRuntime() {
        String dbPath = defaultDbPath();                  // e.g. ~/.zettelkastenfx/zk.db
        this.ds = new SQLiteDatabase("jdbc:sqlite:" + dbPath).migrateAndGetDataSource();
        this.notes = new NoteRepositorySQLite(ds);
        this.keywords = new KeywordRepositorySQLite(ds);
        this.links = new LinkRepositorySQLite(ds);
        this.seq = new SequenceRepositorySQLite(ds);
    }

    private static String defaultDbPath() {
        String home = System.getProperty("user.home");
        Path dir = Paths.get(home, ".zettelkastenfx");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("zk.db").toAbsolutePath().toString();
    }
}
