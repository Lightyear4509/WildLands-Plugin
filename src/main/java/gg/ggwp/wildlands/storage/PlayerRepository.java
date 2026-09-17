package gg.ggwp.wildlands.storage;

import java.sql.*;
import java.util.*;

// Confined to the single storage worker. UUIDs always come from Paper, never names or prefixes.
public final class PlayerRepository {
    private final Database database;
    public PlayerRepository(Database database) { this.database = database; }

    public Optional<PlayerRecord> find(UUID uuid) throws SQLException {
        try (PreparedStatement query = database.connection().prepareStatement(
                "SELECT last_known_name, first_seen, last_seen FROM players WHERE uuid=?")) {
            query.setString(1, uuid.toString());
            try (ResultSet result = query.executeQuery()) {
                return result.next() ? Optional.of(new PlayerRecord(uuid, result.getString(1),
                        result.getLong(2), result.getLong(3))) : Optional.empty();
            }
        }
    }

    public void saveBatch(Collection<PlayerRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        Connection connection = database.connection();
        connection.setAutoCommit(false);
        try (PreparedStatement update = connection.prepareStatement("""
                INSERT INTO players(uuid,last_known_name,first_seen,last_seen) VALUES(?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET
                  last_known_name=CASE WHEN excluded.last_seen >= players.last_seen
                    THEN excluded.last_known_name ELSE players.last_known_name END,
                  first_seen=MIN(players.first_seen,excluded.first_seen),
                  last_seen=MAX(players.last_seen,excluded.last_seen)
                """)) {
            for (PlayerRecord record : records) {
                update.setString(1, record.uuid().toString());
                update.setString(2, record.lastKnownName());
                update.setLong(3, record.firstSeen());
                update.setLong(4, record.lastSeen());
                update.addBatch();
            }
            update.executeBatch();
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        } finally { connection.setAutoCommit(true); }
    }
}
