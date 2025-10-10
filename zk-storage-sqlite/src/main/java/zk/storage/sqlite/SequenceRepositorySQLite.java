package zk.storage.sqlite;

import zk.core.model.NoteId;
import zk.core.ports.SequenceRepository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SequenceRepositorySQLite implements SequenceRepository {
    private final DataSource ds;

    public SequenceRepositorySQLite(DataSource ds) { this.ds = ds; }

    @Override public List<NoteId> childrenOf(NoteId parent) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("SELECT child_id FROM seq_edge WHERE parent_id=? ORDER BY ord ASC")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, parent.value());
            var rs = ps.executeQuery();
            List<NoteId> list = new ArrayList<>();
            while (rs.next()) list.add(new NoteId(rs.getInt(1)));
            rs.close();
            return list;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Optional<NoteId> parentOf(NoteId child) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("SELECT parent_id FROM seq_edge WHERE child_id=?")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, child.value());
            var rs = ps.executeQuery();
            if (rs.next()) return Optional.of(new NoteId(rs.getInt(1)));
            return Optional.empty();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void insertChild(NoteId parent, NoteId child, int order) {
        try (var c = ds.getConnection()) {
            SQLiteDatabase.enableForeignKeys(c);
            // shift existing ord >= order
            try (var shift = c.prepareStatement(
                    "UPDATE seq_edge SET ord = ord + 1 WHERE parent_id=? AND ord>=?")) {
                shift.setInt(1, parent.value()); shift.setInt(2, Math.max(order, 0)); shift.executeUpdate();
            }
            try (var ins = c.prepareStatement(
                    "INSERT OR IGNORE INTO seq_edge(parent_id,child_id,ord) VALUES (?,?,?)")) {
                ins.setInt(1, parent.value());
                ins.setInt(2, child.value());
                ins.setInt(3, Math.max(order, 0));
                ins.executeUpdate();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void reorder(NoteId parent, List<NoteId> orderedChildren) {
        try (var c = ds.getConnection();
             var del = c.prepareStatement("DELETE FROM seq_edge WHERE parent_id=?");
             var ins = c.prepareStatement("INSERT INTO seq_edge(parent_id,child_id,ord) VALUES (?,?,?)")) {
            SQLiteDatabase.enableForeignKeys(c);
            del.setInt(1, parent.value());
            del.executeUpdate();
            for (int i = 0; i < orderedChildren.size(); i++) {
                ins.setInt(1, parent.value());
                ins.setInt(2, orderedChildren.get(i).value());
                ins.setInt(3, i);
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void detach(NoteId child) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("DELETE FROM seq_edge WHERE child_id=?")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, child.value());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
