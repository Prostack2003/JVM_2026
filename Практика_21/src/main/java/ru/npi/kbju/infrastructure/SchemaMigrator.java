package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Последовательно и однократно применяет включённые в проект версии схемы. */
final class SchemaMigrator {
    static final String V001_RESOURCE =
            "ru/npi/kbju/infrastructure/db/migration/V001__nutrition_plans.sql";
    static final String V002_RESOURCE =
            "ru/npi/kbju/infrastructure/db/migration/V002__add_import_code.sql";

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "initial nutrition plans", V001_RESOURCE),
            new Migration(2, "add import code", V002_RESOURCE));

    private SchemaMigrator() {
    }

    /** Создаёт журнал версий и применяет только отсутствующие миграции по порядку. */
    static MigrationReport apply(Connection connection) throws SQLException, IOException {
        Objects.requireNonNull(connection, "Соединение обязательно");
        if (!connection.getAutoCommit()) {
            throw new SQLException("Миграции запускаются вне пользовательской транзакции");
        }
        createHistory(connection);
        var previouslyApplied = appliedVersions(connection);
        var appliedNow = new ArrayList<Integer>();
        for (var migration : MIGRATIONS) {
            if (!previouslyApplied.contains(migration.version())) {
                applyOne(connection, migration);
                appliedNow.add(migration.version());
            }
        }
        return new MigrationReport(currentVersion(connection), List.copyOf(appliedNow));
    }

    /** Возвращает максимальную зарегистрированную версию либо ноль. */
    static int currentVersion(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(version), 0) FROM schema_migration");
             var rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    /** Возвращает число зарегистрированных шагов для проверки повторного запуска. */
    static int appliedCount(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM schema_migration");
             var rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    private static void createHistory(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migration (
                        version INTEGER PRIMARY KEY,
                        description TEXT NOT NULL,
                        applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    private static List<Integer> appliedVersions(Connection connection) throws SQLException {
        var result = new ArrayList<Integer>();
        try (var statement = connection.prepareStatement(
                "SELECT version FROM schema_migration ORDER BY version");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(rows.getInt(1));
            }
        }
        return List.copyOf(result);
    }

    private static void applyOne(Connection connection, Migration migration)
            throws SQLException, IOException {
        Throwable pending = null;
        connection.setAutoCommit(false);
        try {
            executeScript(connection, readResource(migration.resource()));
            try (var insert = connection.prepareStatement(
                    "INSERT INTO schema_migration(version, description) VALUES (?, ?)")) {
                insert.setInt(1, migration.version());
                insert.setString(2, migration.description());
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | IOException | RuntimeException exception) {
            pending = exception;
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException restoreFailure) {
                if (pending != null) {
                    pending.addSuppressed(restoreFailure);
                } else {
                    throw restoreFailure;
                }
            }
        }
    }

    private static void executeScript(Connection connection, String script) throws SQLException {
        for (var sql : script.split(";")) {
            if (!sql.isBlank()) {
                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }

    static String readResource(String resource) throws IOException {
        try (var stream = openResource(resource)) {
            if (stream == null) {
                throw new IOException("Миграция не найдена: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream openResource(String resource) {
        var fromModule = SchemaMigrator.class.getResourceAsStream("/" + resource);
        return fromModule != null
                ? fromModule
                : SchemaMigrator.class.getClassLoader().getResourceAsStream(resource);
    }

    /** Фактическая версия и список шагов, выполненных именно этим запуском. */
    record MigrationReport(int currentVersion, List<Integer> appliedNow) {
        MigrationReport {
            appliedNow = List.copyOf(appliedNow);
        }
    }

    private record Migration(int version, String description, String resource) {
    }
}
