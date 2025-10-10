package zk.storage.sqlite;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

public final class SQLiteDatabase {

    private final String jdbcUrl;

    public SQLiteDatabase(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public DataSource migrateAndGetDataSource() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(jdbcUrl);

        // Run Flyway migrations against this URL
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        return ds;
    }

    /** Utility: enable foreign keys for a fresh connection. */
    static void enableForeignKeys(java.sql.Connection c) {
        try (var st = c.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
        catch (Exception ignored) { }
    }
}
