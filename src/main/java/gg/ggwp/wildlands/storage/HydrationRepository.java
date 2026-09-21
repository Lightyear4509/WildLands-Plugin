package gg.ggwp.wildlands.storage;

import java.sql.*;
import java.util.*;

/** Confined to the storage worker, with whole-snapshot, last-queued-write semantics. */
public final class HydrationRepository {
    private final Database database;
    public HydrationRepository(Database database) { this.database = database; }

    public Optional<HydrationRecord> find(UUID uuid) throws SQLException {
        try (PreparedStatement query = database.connection().prepareStatement(
                "SELECT hydration,hud_enabled,dry_seconds FROM hydration_players WHERE uuid=?")) {
            query.setString(1, uuid.toString());
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) return Optional.empty();
                try {
                    return Optional.of(new HydrationRecord(uuid, row.getDouble(1), row.getBoolean(2), row.getDouble(3)));
                } catch (IllegalArgumentException invalid) {
                    throw new SQLException("Invalid hydration state for " + uuid, invalid);
                }
            }
        }
    }

    public void saveBatch(Collection<HydrationRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        Connection connection = database.connection();
        connection.setAutoCommit(false);
        try (PreparedStatement update = connection.prepareStatement("""
                INSERT INTO hydration_players(uuid,hydration,hud_enabled,dry_seconds) VALUES(?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET hydration=excluded.hydration,
                  hud_enabled=excluded.hud_enabled,dry_seconds=excluded.dry_seconds
                """)) {
            for (HydrationRecord record : records) {
                update.setString(1, record.uuid().toString());
                update.setDouble(2, record.hydration());
                update.setBoolean(3, record.hudEnabled());
                update.setDouble(4, record.drySeconds());
                update.addBatch();
            }
            update.executeBatch();
            connection.commit();
        } catch (SQLException failure) {
            try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        } finally { connection.setAutoCommit(true); }
    }
}
