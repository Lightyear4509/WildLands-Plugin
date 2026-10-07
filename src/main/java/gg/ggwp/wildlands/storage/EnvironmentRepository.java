package gg.ggwp.wildlands.storage;

import java.sql.*;
import java.util.*;

final class EnvironmentRepository {
    private final Database database;
    EnvironmentRepository(Database database) { this.database = database; }
    Optional<EnvironmentRecord> find(UUID id) throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT temperature,wetness FROM environment_players WHERE uuid=?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(new EnvironmentRecord(id, result.getDouble(1), result.getDouble(2))) : Optional.empty();
            }
        }
    }
    void saveBatch(Collection<EnvironmentRecord> records) throws SQLException {
        if (records.isEmpty()) return;
        Connection connection = database.connection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO environment_players(uuid,temperature,wetness) VALUES(?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET temperature=excluded.temperature,wetness=excluded.wetness
                """)) {
            for (EnvironmentRecord record : records) {
                statement.setString(1, record.uuid().toString());
                statement.setDouble(2, record.temperature());
                statement.setDouble(3, record.wetness());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        } finally { connection.setAutoCommit(previous); }
    }
}
