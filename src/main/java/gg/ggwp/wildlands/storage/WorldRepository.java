package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.config.WorldgenSettings;
import java.sql.*;
import java.util.*;

/** Frozen profiles prevent configuration changes from creating generation seams. */
final class WorldRepository {
    private final Database database;
    WorldRepository(Database database) { this.database = database; }
    List<WorldRecord> all() throws SQLException {
        var records = new ArrayList<WorldRecord>();
        try (var query = database.connection().prepareStatement("SELECT * FROM wildlands_worlds ORDER BY name"); var rows = query.executeQuery()) {
            while (rows.next()) {
                try {
                    String uuid = rows.getString("uuid");
                    records.add(new WorldRecord(rows.getString("name"), uuid == null ? null : UUID.fromString(uuid), rows.getLong("seed"),
                            new WorldgenSettings(rows.getInt("generator_version"), rows.getInt("sea_level"), rows.getDouble("tree_density"),
                                    rows.getInt("caves") == 1, rows.getInt("ores") == 1)));
                } catch (IllegalArgumentException invalid) { throw new SQLException("Invalid persisted generator profile", invalid); }
                if (records.size() > 16) throw new SQLException("At most 16 registered rainforest worlds are supported");
            }
        }
        return List.copyOf(records);
    }
    void reserve(WorldRecord record) throws SQLException {
        try (var insert = database.connection().prepareStatement("""
                INSERT INTO wildlands_worlds(name,uuid,seed,generator_version,sea_level,tree_density,caves,ores)
                VALUES(?,?,?,?,?,?,?,?)
                """)) {
            insert.setString(1, record.name()); insert.setString(2, record.uuid() == null ? null : record.uuid().toString());
            insert.setLong(3, record.seed()); insert.setInt(4, record.settings().version()); insert.setInt(5, record.settings().seaLevel());
            insert.setDouble(6, record.settings().treeDensity()); insert.setInt(7, record.settings().caves() ? 1 : 0);
            insert.setInt(8, record.settings().ores() ? 1 : 0); insert.executeUpdate();
        }
    }
    void identify(WorldRecord record) throws SQLException {
        if (record.uuid() == null) throw new IllegalArgumentException("Missing world UUID");
        try (var update = database.connection().prepareStatement("UPDATE wildlands_worlds SET uuid=? WHERE name=? AND (uuid IS NULL OR uuid=?)")) {
            update.setString(1, record.uuid().toString()); update.setString(2, record.name()); update.setString(3, record.uuid().toString());
            if (update.executeUpdate() != 1) throw new SQLException("World identity mismatch for " + record.name());
        }
    }
    void identifyBatch(Collection<WorldRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        var connection = database.connection(); connection.setAutoCommit(false);
        try {
            for (var record : records) identify(record);
            connection.commit();
        } catch (SQLException failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }
}
