package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Проверяет новую базу, повторный запуск и обновление файла практики 11. */
class SchemaMigratorTest {
    @TempDir
    Path directory;

    @Test
    void freshDatabaseAppliesEachMigrationExactlyOnce() throws Exception {
        var database = directory.resolve("fresh.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            var first = SchemaMigrator.apply(connection);
            var second = SchemaMigrator.apply(connection);

            assertEquals(2, first.currentVersion());
            assertEquals(java.util.List.of(1, 2), first.appliedNow());
            assertEquals(2, second.currentVersion());
            assertTrue(second.appliedNow().isEmpty());
            assertEquals(2, SchemaMigrator.appliedCount(connection));
            assertTrue(hasColumn(connection, "nutrition_plan", "import_code"));
        }
    }

    @Test
    void practiceElevenDatabaseKeepsOldRowWhenMigratedToV002() throws Exception {
        var database = directory.resolve("old-v1.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            try (var statement = connection.createStatement()) {
                statement.execute(SchemaMigrator.readResource(
                        SchemaMigrator.V001_RESOURCE));
            }
            try (var insert = connection.prepareStatement("""
                    INSERT INTO nutrition_plan(name, name_key, effective_from, status)
                    VALUES (?, ?, ?, ?)
                    """)) {
                insert.setString(1, "Старый план");
                insert.setString(2, "старый план");
                insert.setString(3, "2026-10-01");
                insert.setString(4, "ACTIVE");
                assertEquals(1, insert.executeUpdate());
            }
        }

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            assertEquals(2, repository.schemaVersion());
            assertEquals(2, repository.appliedMigrationCount());
            assertEquals("Старый план", repository.findAll().getFirst().name());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var query = connection.prepareStatement(
                     "SELECT import_code FROM nutrition_plan WHERE name_key = ?")) {
            query.setString(1, "старый план");
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                assertNull(rows.getString(1));
            }
        }
    }

    private static boolean hasColumn(
            java.sql.Connection connection,
            String table,
            String expected
    ) throws Exception {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (expected.equals(rows.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
