package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.seasons.*;
import java.sql.*;
import java.util.*;

final class SeasonRepository {
    private final Database database;
    SeasonRepository(Database database) { this.database = database; }

    Optional<SeasonRecord> find(UUID worldUuid) throws SQLException {
        try (PreparedStatement query = database.connection().prepareStatement(
                "SELECT season,elapsed_ticks FROM season_state WHERE world_uuid=?")) {
            query.setString(1, worldUuid.toString());
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                try {
                    return Optional.of(new SeasonRecord(worldUuid,
                            new SeasonClock.State(Season.valueOf(rows.getString(1)), rows.getLong(2))));
                } catch (IllegalArgumentException | NullPointerException invalid) {
                    throw new SQLException("Invalid persisted season for " + worldUuid, invalid);
                }
            }
        }
    }

    void saveBatch(Collection<SeasonRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        Connection connection = database.connection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO season_state(world_uuid,season,elapsed_ticks) VALUES(?,?,?)
                ON CONFLICT(world_uuid) DO UPDATE SET season=excluded.season,elapsed_ticks=excluded.elapsed_ticks
                """)) {
            for (SeasonRecord record : records) {
                insert.setString(1, record.worldUuid().toString());
                insert.setString(2, record.state().season().name());
                insert.setLong(3, record.state().elapsedTicks());
                insert.addBatch();
            }
            insert.executeBatch();
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback(); throw failure;
        } finally { connection.setAutoCommit(previous); }
    }
}
