package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.world.LandmarkKind;
import java.sql.*;
import java.util.*;

final class ExplorationRepository {
    private final Database database;
    ExplorationRepository(Database database) { this.database = database; }
    List<LandmarkRecord> landmarks() throws SQLException {
        var result = new ArrayList<LandmarkRecord>();
        try (var query = database.connection().prepareStatement("SELECT * FROM landmarks ORDER BY id"); var rows = query.executeQuery()) {
            while (rows.next()) {
                try {
                    String owner = rows.getString("owner_uuid");
                    result.add(new LandmarkRecord(UUID.fromString(rows.getString("id")), UUID.fromString(rows.getString("world_uuid")),
                            rows.getString("name"), LandmarkKind.valueOf(rows.getString("kind")), rows.getInt("x"), rows.getInt("y"), rows.getInt("z"),
                            owner == null ? null : UUID.fromString(owner)));
                } catch (IllegalArgumentException invalid) { throw new SQLException("Invalid saved landmark", invalid); }
                if (result.size() > 131072) throw new SQLException("Landmark registry exceeds supported 131072 records");
            }
        }
        return List.copyOf(result);
    }
    Map<UUID, Long> discoveries(UUID player) throws SQLException {
        var result = new LinkedHashMap<UUID, Long>();
        try (var query = database.connection().prepareStatement("SELECT landmark_id,first_seen FROM discoveries WHERE player_uuid=? ORDER BY first_seen")) {
            query.setString(1, player.toString());
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    try { result.put(UUID.fromString(rows.getString(1)), rows.getLong(2)); }
                    catch (IllegalArgumentException invalid) { throw new SQLException("Invalid saved discovery", invalid); }
                    if (result.size() > 65536) throw new SQLException("Player discovery journal exceeds supported capacity");
                }
            }
        }
        return Map.copyOf(result);
    }
    Optional<ExpeditionRecord> player(UUID id) throws SQLException {
        try (var query = database.connection().prepareStatement("SELECT * FROM exploration_players WHERE uuid=?")) {
            query.setString(1, id.toString());
            try (var rows = query.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                try {
                    String target = rows.getString("target_id");
                    return Optional.of(new ExpeditionRecord(id, target == null ? null : UUID.fromString(target), rows.getDouble("longest_distance"),
                            rows.getInt("completed_trips"), rows.getInt("active") == 1, ExpeditionRecord.Rank.valueOf(rows.getString("rank"))));
                } catch (IllegalArgumentException invalid) { throw new SQLException("Invalid saved expedition", invalid); }
            }
        }
    }
    void saveBatch(Collection<LandmarkRecord> landmarks, Collection<DiscoveryRecord> discoveries,
                   Collection<ExpeditionRecord> players) throws SQLException {
        if (landmarks.isEmpty() && discoveries.isEmpty() && players.isEmpty()) return;
        var connection = database.connection(); connection.setAutoCommit(false);
        try (var landmark = connection.prepareStatement("""
                INSERT INTO landmarks(id,world_uuid,name,kind,x,y,z,owner_uuid) VALUES(?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET world_uuid=excluded.world_uuid,name=excluded.name,kind=excluded.kind,
                    x=excluded.x,y=excluded.y,z=excluded.z,owner_uuid=excluded.owner_uuid
                """); var discovery = connection.prepareStatement("""
                INSERT INTO discoveries(player_uuid,landmark_id,first_seen) VALUES(?,?,?)
                ON CONFLICT(player_uuid,landmark_id) DO UPDATE SET first_seen=MIN(discoveries.first_seen,excluded.first_seen)
                """); var player = connection.prepareStatement("""
                INSERT INTO exploration_players(uuid,target_id,longest_distance,completed_trips,active,rank) VALUES(?,?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET target_id=excluded.target_id,longest_distance=excluded.longest_distance,
                    completed_trips=excluded.completed_trips,active=excluded.active,rank=excluded.rank
                """)) {
            for (var record : landmarks) {
                landmark.setString(1, record.id().toString()); landmark.setString(2, record.worldUuid().toString());
                landmark.setString(3, record.name()); landmark.setString(4, record.kind().name());
                landmark.setInt(5, record.x()); landmark.setInt(6, record.y()); landmark.setInt(7, record.z());
                landmark.setString(8, record.owner() == null ? null : record.owner().toString()); landmark.addBatch();
            }
            landmark.executeBatch();
            for (var record : discoveries) {
                discovery.setString(1, record.player().toString()); discovery.setString(2, record.landmark().toString());
                discovery.setLong(3, record.firstSeen()); discovery.addBatch();
            }
            discovery.executeBatch();
            for (var record : players) {
                player.setString(1, record.player().toString()); player.setString(2, record.target() == null ? null : record.target().toString());
                player.setDouble(3, record.longestDistance()); player.setInt(4, record.completedTrips());
                player.setInt(5, record.active() ? 1 : 0); player.setString(6, record.rank().name()); player.addBatch();
            }
            player.executeBatch(); connection.commit();
        } catch (SQLException failure) { connection.rollback(); throw failure; }
        finally { connection.setAutoCommit(true); }
    }
}
