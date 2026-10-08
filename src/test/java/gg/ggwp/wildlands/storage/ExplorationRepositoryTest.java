package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.world.LandmarkKind;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExplorationRepositoryTest {
    @TempDir Path directory;
    @Test void journalNavigationCampAndProgressSurviveRestartAndUuidIsolation() throws Exception {
        Path file = directory.resolve("exploration.db"); UUID player = UUID.fromString("00000000-0000-0000-0009-01fe76725ed6"), other = UUID.randomUUID(), world = UUID.randomUUID();
        var landmark = new LandmarkRecord(UUID.randomUUID(), world, "river-123", LandmarkKind.RIVER_BEND, 10, 65, -10, null);
        var camp = new LandmarkRecord(UUID.randomUUID(), world, "camp-123", LandmarkKind.CAMP, 0, 65, 0, player);
        var state = new ExpeditionRecord(player, landmark.id(), 1024, 2, false, ExpeditionRecord.Rank.EXPLORER);
        try (var db = new Database()) {
            db.open(file); var repo = new ExplorationRepository(db);
            repo.saveBatch(List.of(landmark, camp), List.of(new DiscoveryRecord(player, landmark.id(), 100)), List.of(state));
            repo.saveBatch(List.of(), List.of(new DiscoveryRecord(player, landmark.id(), 200), new DiscoveryRecord(other, landmark.id(), 300)), List.of());
        }
        try (var db = new Database()) {
            db.open(file); var repo = new ExplorationRepository(db); assertEquals(2, repo.landmarks().size()); assertEquals(state, repo.player(player).orElseThrow());
            assertEquals(Map.of(landmark.id(), 100L), repo.discoveries(player)); assertEquals(Map.of(landmark.id(), 300L), repo.discoveries(other));
            assertTrue(repo.player(other).isEmpty());
        }
    }
    @Test void malformedDiscoveryRollsBackTheWholeLandmarkAndPlayerBatch() throws Exception {
        try (var db = new Database()) {
            db.open(directory.resolve("atomic.db")); var repo = new ExplorationRepository(db); UUID player = UUID.randomUUID();
            var record = new LandmarkRecord(UUID.randomUUID(), UUID.randomUUID(), "ruin", LandmarkKind.RUINS, 1, 65, 1, null);
            assertThrows(SQLException.class, () -> repo.saveBatch(List.of(record), List.of(new DiscoveryRecord(player, UUID.randomUUID(), 1)), List.of(ExpeditionRecord.initial(player))));
            assertTrue(repo.landmarks().isEmpty()); assertTrue(repo.player(player).isEmpty());
        }
    }
    @Test void schemaSixMigrationKeepsFrozenWorldAndWildlifeThenAllowsVersionTwo() throws Exception {
        Path file = directory.resolve("schema-six.db"); UUID world = UUID.randomUUID(), cat = UUID.randomUUID();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var sql = connection.createStatement()) {
            for (String ddl : new String[]{
                    "CREATE TABLE players(uuid TEXT PRIMARY KEY,last_known_name TEXT,first_seen INTEGER,last_seen INTEGER)",
                    "CREATE TABLE hydration_players(uuid TEXT PRIMARY KEY,hydration REAL,hud_enabled INTEGER,dry_seconds REAL)",
                    "CREATE TABLE environment_players(uuid TEXT PRIMARY KEY,temperature REAL,wetness REAL)",
                    "CREATE TABLE season_state(world_uuid TEXT PRIMARY KEY,season TEXT,elapsed_ticks INTEGER)",
                    "CREATE TABLE wildlands_worlds(name TEXT PRIMARY KEY,uuid TEXT UNIQUE,seed INTEGER,generator_version INTEGER CHECK(generator_version=1),sea_level INTEGER,tree_density REAL,caves INTEGER,ores INTEGER)",
                    "CREATE TABLE wildlife(uuid TEXT PRIMARY KEY,world_uuid TEXT,home_x REAL,home_y REAL,home_z REAL,alive INTEGER)"}) sql.execute(ddl);
            try (var insert = connection.prepareStatement("INSERT INTO wildlands_worlds VALUES('rainforest',?,4509,1,63,.8,1,1)")) { insert.setString(1, world.toString()); insert.executeUpdate(); }
            try (var insert = connection.prepareStatement("INSERT INTO wildlife VALUES(?,?,1,65,1,1)")) { insert.setString(1, cat.toString()); insert.setString(2, world.toString()); insert.executeUpdate(); }
            sql.execute("PRAGMA user_version=6");
        }
        try (var db = new Database()) {
            db.open(file); var worlds = new WorldRepository(db); var old = worlds.all().getFirst();
            assertEquals(world, old.uuid()); assertEquals(4509, old.seed()); assertEquals(1, old.settings().version());
            assertEquals(cat, new WildlifeRepository(db).living().getFirst().uuid());
            worlds.reserve(new WorldRecord("newforest", null, 1, new gg.ggwp.wildlands.config.WorldgenSettings(2, 63, .8, true, true)));
            assertEquals(2, worlds.all().size()); assertTrue(new ExplorationRepository(db).landmarks().isEmpty());
        }
    }
}
