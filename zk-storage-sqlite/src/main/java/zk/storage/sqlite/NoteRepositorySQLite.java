package zk.storage.sqlite;

import zk.core.model.NoteDTO;
import zk.core.model.NoteId;
import zk.core.ports.NoteRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class NoteRepositorySQLite implements NoteRepository {
    private final DataSource ds;

    public NoteRepositorySQLite(DataSource ds) { this.ds = ds; }

    @Override public Optional<NoteDTO> get(NoteId id) {
        try (var c = ds.getConnection()) {
            SQLiteDatabase.enableForeignKeys(c);
            try (var ps = c.prepareStatement(
                    "SELECT id,title,body,created_at,modified_at,rating FROM note WHERE id=?")) {
                ps.setInt(1, id.value());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(map(rs));
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public NoteId create(String title, String body) {
        Instant now = Instant.now();
        try (var c = ds.getConnection()) {
            SQLiteDatabase.enableForeignKeys(c);
            try (var ps = c.prepareStatement(
                    "INSERT INTO note(title,body,created_at,modified_at,rating) VALUES (?,?,?,?,0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);
                ps.setString(2, body);
                ps.setString(3, now.toString());
                ps.setString(4, now.toString());
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new NoteId(keys.getInt(1));
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void update(NoteDTO n) {
        try (var c = ds.getConnection()) {
            SQLiteDatabase.enableForeignKeys(c);
            try (var ps = c.prepareStatement(
                    "UPDATE note SET title=?, body=?, modified_at=?, rating=? WHERE id=?")) {
                ps.setString(1, n.title());
                ps.setString(2, n.body());
                ps.setString(3, n.modifiedAt().toString());
                ps.setInt(4, n.rating());
                ps.setInt(5, n.id().value());
                ps.executeUpdate();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public void delete(NoteId id) {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("DELETE FROM note WHERE id=?")) {
            SQLiteDatabase.enableForeignKeys(c);
            ps.setInt(1, id.value());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Stream<NoteDTO> all() {
        try {
            var c = ds.getConnection(); // release via onClose handler below
            SQLiteDatabase.enableForeignKeys(c);
            var st = c.createStatement();
            var rs = st.executeQuery("SELECT id,title,body,created_at,modified_at,rating FROM note ORDER BY id ASC");

            var spliterator = new java.util.Spliterators.AbstractSpliterator<NoteDTO>(
                    Long.MAX_VALUE, java.util.Spliterator.ORDERED) {
                @Override public boolean tryAdvance(java.util.function.Consumer<? super NoteDTO> action) {
                    try {
                        if (!rs.next()) return false;
                        action.accept(map(rs));
                        return true;
                    } catch (SQLException e) { throw new RuntimeException(e); }
                }
            };

            return StreamSupport.stream(spliterator, false)
                    .onClose(() -> { try { rs.close(); st.close(); c.close(); } catch (Exception ignored) {} });

        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static NoteDTO map(ResultSet rs) throws SQLException {
        return new NoteDTO(
                new NoteId(rs.getInt("id")),
                rs.getString("title"),
                rs.getString("body"),
                Instant.parse(rs.getString("created_at")),
                Instant.parse(rs.getString("modified_at")),
                rs.getInt("rating")
        );
    }
}
