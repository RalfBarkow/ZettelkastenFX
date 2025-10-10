package zk.storage.sqlite;

import zk.core.model.NoteId;
import zk.core.ports.KeywordRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class KeywordRepositorySQLite implements KeywordRepository {
    private final DataSource ds;

    public KeywordRepositorySQLite(DataSource ds) { this.ds = ds; }

    @Override public Set<String> getFor(NoteId id) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("SELECT name FROM note_keyword WHERE note_id=? ORDER BY name ASC")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, id.value());
            try (var rs = ps.executeQuery()) {
                Set<String> set = new LinkedHashSet<>();
                while (rs.next()) set.add(rs.getString(1));
                return set;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void add(NoteId id, String kw) {
        try (Connection c = ds.getConnection()) {
            SQLiteDatabase.enableForeignKeys(c);
            try (var ps1 = c.prepareStatement("INSERT OR IGNORE INTO keyword(name) VALUES (?)");
                 var ps2 = c.prepareStatement("INSERT OR IGNORE INTO note_keyword(note_id,name) VALUES (?,?)")) {
                ps1.setString(1, kw); ps1.executeUpdate();
                ps2.setInt(1, id.value()); ps2.setString(2, kw); ps2.executeUpdate();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void remove(NoteId id, String kw) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("DELETE FROM note_keyword WHERE note_id=? AND name=?")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, id.value());
            ps.setString(2, kw);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Stream<String> all() {
        try {
            var c = ds.getConnection();
            SQLiteDatabase.enableForeignKeys(c);
            var st = c.createStatement();
            var rs = st.executeQuery("SELECT name FROM keyword ORDER BY name ASC");
            java.util.List<String> list = new java.util.ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            rs.close(); st.close(); c.close();
            return list.stream();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
