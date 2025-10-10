package zk.storage.sqlite;

import zk.core.model.NoteId;
import zk.core.ports.LinkRepository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.stream.Stream;

public final class LinkRepositorySQLite implements LinkRepository {
    private final DataSource ds;

    public LinkRepositorySQLite(DataSource ds) { this.ds = ds; }

    @Override public Stream<NoteId> outgoing(NoteId from) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("SELECT dst_id FROM link WHERE src_id=? ORDER BY dst_id ASC")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, from.value());
            var rs = ps.executeQuery();
            var list = new ArrayList<NoteId>();
            while (rs.next()) list.add(new NoteId(rs.getInt(1)));
            rs.close();
            return list.stream();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Stream<NoteId> incoming(NoteId to) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("SELECT src_id FROM link WHERE dst_id=? ORDER BY src_id ASC")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, to.value());
            var rs = ps.executeQuery();
            var list = new ArrayList<NoteId>();
            while (rs.next()) list.add(new NoteId(rs.getInt(1)));
            rs.close();
            return list.stream();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void add(NoteId from, NoteId to) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("INSERT OR IGNORE INTO link(src_id,dst_id) VALUES (?,?)")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, from.value());
            ps.setInt(2, to.value());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void remove(NoteId from, NoteId to) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("DELETE FROM link WHERE src_id=? AND dst_id=?")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, from.value());
            ps.setInt(2, to.value());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
