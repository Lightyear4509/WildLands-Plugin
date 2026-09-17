package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaValidationTest {
    @TempDir Path directory;
    @Test void refusesVersionedDatabaseWithMissingTables() throws Exception {
        Path file = directory.resolve("incomplete.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) { statement.execute("PRAGMA user_version=1"); }
        try (var db = new Database()) { assertThrows(SQLException.class, () -> db.open(file)); }
    }
}
