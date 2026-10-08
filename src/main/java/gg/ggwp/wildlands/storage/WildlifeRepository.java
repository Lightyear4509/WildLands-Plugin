package gg.ggwp.wildlands.storage;

import java.sql.*;
import java.util.*;

final class WildlifeRepository {
    private final Database database;
    WildlifeRepository(Database database) { this.database = database; }
    List<WildlifeRecord> living() throws SQLException {
        var records = new ArrayList<WildlifeRecord>();
        try (var query = database.connection().prepareStatement("SELECT * FROM wildlife WHERE alive=1"); var rows = query.executeQuery()) {
            while (rows.next()) {
                try { records.add(new WildlifeRecord(UUID.fromString(rows.getString("uuid")), UUID.fromString(rows.getString("world_uuid")),
                        rows.getDouble("home_x"), rows.getDouble("home_y"), rows.getDouble("home_z"), true)); }
                catch (IllegalArgumentException failure) { throw new SQLException("Invalid wildlife record", failure); }
                if (records.size() > 4096) throw new SQLException("Wildlife registry exceeds 4096 animals");
            }
        }
        return List.copyOf(records);
    }
    void saveBatch(Collection<WildlifeRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        var connection = database.connection(); connection.setAutoCommit(false);
        try (var upsert = connection.prepareStatement("""
                INSERT INTO wildlife(uuid,world_uuid,home_x,home_y,home_z,alive) VALUES(?,?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET world_uuid=excluded.world_uuid,home_x=excluded.home_x,
                  home_y=excluded.home_y,home_z=excluded.home_z,alive=excluded.alive
                """)) {
            for (var record : records) {
                upsert.setString(1, record.uuid().toString()); upsert.setString(2, record.worldUuid().toString());
                upsert.setDouble(3, record.homeX()); upsert.setDouble(4, record.homeY()); upsert.setDouble(5, record.homeZ());
                upsert.setInt(6, record.alive() ? 1 : 0); upsert.addBatch();
            }
            upsert.executeBatch();
            // Removal and registration snapshots share the same serial worker; remove dead registry entries.
            try (var cleanup = connection.createStatement()) { cleanup.executeUpdate("DELETE FROM wildlife WHERE alive=0"); }
            connection.commit();
        } catch (SQLException failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }
}
