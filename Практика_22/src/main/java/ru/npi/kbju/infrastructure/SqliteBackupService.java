package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Создаёт согласованный SQLite-снимок и восстанавливает его только в новый файл. */
public final class SqliteBackupService {
    public static final int SUPPORTED_SCHEMA_VERSION = 2;

    private final Clock clock;

    /** Использует системные часы UTC для метаданных снимка. */
    public SqliteBackupService() {
        this(Clock.systemUTC());
    }

    /** Позволяет проверкам зафиксировать время без ожиданий и пауз. */
    public SqliteBackupService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Часы обязательны");
    }

    /**
     * Создаёт снимок командой SQLite, проверяет его и только затем публикует
     * по целевому пути. Существующий файл никогда не перезаписывается.
     */
    public BackupSnapshot create(Path sourceDatabase, Path targetDatabase)
            throws BackupException {
        var source = requireRegularFile(sourceDatabase);
        var target = normalize(targetDatabase, "Путь резервной копии обязателен");
        rejectSamePath(source, target);
        rejectExistingTarget(target);
        var sourceInspection = inspect(source);
        var parent = requireParent(target);
        createDirectories(parent, BackupException.Reason.SNAPSHOT_FAILED);

        Path staged = null;
        try {
            staged = Files.createTempFile(parent, ".backup-", ".db");
            Files.delete(staged);
            createSnapshotWithSqlite(source, staged);
            var stagedInspection = inspect(staged);
            requireSameSnapshot(sourceInspection, stagedInspection);
            moveNewFile(staged, target);
            staged = null;
            var publishedInspection = inspect(target);
            requireSameSnapshot(sourceInspection, publishedInspection);
            return new BackupSnapshot(target, clock.instant(), publishedInspection);
        } catch (BackupException exception) {
            deleteUnpublished(staged, exception);
            throw exception;
        } catch (IOException exception) {
            deleteUnpublished(staged, exception);
            throw new BackupException(
                    BackupException.Reason.SNAPSHOT_FAILED,
                    "Не удалось опубликовать резервную копию SQLite",
                    exception
            );
        }
    }

    /**
     * Проверяет копию, переносит её через промежуточный файл и отказывается
     * заменять любой существующий файл, включая рабочую базу.
     */
    public RestoreResult restoreToNewDatabase(Path backupDatabase, Path targetDatabase)
            throws BackupException {
        var backup = requireRegularFile(backupDatabase);
        var target = normalize(targetDatabase, "Тестовый путь восстановления обязателен");
        rejectSamePath(backup, target);
        rejectExistingTarget(target);
        var backupInspection = inspect(backup);
        var parent = requireParent(target);
        createDirectories(parent, BackupException.Reason.RESTORE_FAILED);

        Path staged = null;
        try {
            staged = Files.createTempFile(parent, ".restore-", ".db");
            Files.copy(backup, staged, StandardCopyOption.REPLACE_EXISTING);
            var stagedInspection = inspect(staged);
            requireSameSnapshot(backupInspection, stagedInspection);
            moveNewFile(staged, target);
            staged = null;
            var restoredInspection = inspect(target);
            requireSameSnapshot(backupInspection, restoredInspection);
            return new RestoreResult(backup, target, restoredInspection);
        } catch (BackupException exception) {
            deleteUnpublished(staged, exception);
            throw exception;
        } catch (IOException exception) {
            deleteUnpublished(staged, exception);
            throw new BackupException(
                    BackupException.Reason.RESTORE_FAILED,
                    "Не удалось восстановить резервную копию в новый файл",
                    exception
            );
        }
    }

    /** Открывает базу отдельно и проверяет целостность, версию схемы и число планов. */
    public DatabaseInspection inspect(Path database) throws BackupException {
        var file = requireRegularFile(database);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var queryOnly = connection.createStatement()) {
            queryOnly.execute("PRAGMA query_only = ON");

            final String integrity;
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("PRAGMA integrity_check")) {
                integrity = rows.next() ? rows.getString(1) : "no result";
                if (!"ok".equalsIgnoreCase(integrity) || rows.next()) {
                    throw new BackupException(
                            BackupException.Reason.INTEGRITY_FAILED,
                            "Проверка целостности SQLite не вернула единственный результат ok"
                    );
                }
            }

            final int schemaVersion;
            try (var statement = connection.prepareStatement(
                    "SELECT COALESCE(MAX(version), 0) FROM schema_migration");
                 var rows = statement.executeQuery()) {
                schemaVersion = rows.next() ? rows.getInt(1) : 0;
            } catch (SQLException exception) {
                throw new BackupException(
                        BackupException.Reason.UNSUPPORTED_SCHEMA,
                        "В базе отсутствует журнал версий схемы",
                        exception
                );
            }
            if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                throw new BackupException(
                        BackupException.Reason.UNSUPPORTED_SCHEMA,
                        "Версия схемы резервной копии не поддерживается: " + schemaVersion
                );
            }

            final int planCount;
            try (var statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM nutrition_plan");
                 var rows = statement.executeQuery()) {
                planCount = rows.next() ? rows.getInt(1) : 0;
            } catch (SQLException exception) {
                throw new BackupException(
                        BackupException.Reason.UNSUPPORTED_SCHEMA,
                        "В базе отсутствует таблица планов питания",
                        exception
                );
            }
            return new DatabaseInspection(file, integrity, schemaVersion, planCount);
        } catch (BackupException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new BackupException(
                    BackupException.Reason.INTEGRITY_FAILED,
                    "Не удалось открыть файл как базу SQLite",
                    exception
            );
        }
    }

    private static void createSnapshotWithSqlite(Path source, Path staged)
            throws BackupException {
        var escapedTarget = staged.toString().replace("'", "''");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + source);
             var statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + escapedTarget + "'");
        } catch (SQLException exception) {
            throw new BackupException(
                    BackupException.Reason.SNAPSHOT_FAILED,
                    "SQLite не смогла создать согласованный снимок",
                    exception
            );
        }
    }

    private static Path requireRegularFile(Path path) throws BackupException {
        var file = normalize(path, "Путь базы обязателен");
        if (!Files.isRegularFile(file)) {
            throw new BackupException(
                    BackupException.Reason.SOURCE_NOT_FOUND,
                    "Файл базы не найден"
            );
        }
        return file;
    }

    private static Path normalize(Path path, String message) {
        return Objects.requireNonNull(path, message).toAbsolutePath().normalize();
    }

    private static Path requireParent(Path target) throws BackupException {
        var parent = target.getParent();
        if (parent == null) {
            throw new BackupException(
                    BackupException.Reason.RESTORE_FAILED,
                    "У целевого пути отсутствует родительский каталог"
            );
        }
        return parent;
    }

    private static void rejectSamePath(Path source, Path target) throws BackupException {
        if (source.equals(target)) {
            throw new BackupException(
                    BackupException.Reason.SAME_PATH,
                    "Источник и целевой файл должны различаться"
            );
        }
    }

    private static void rejectExistingTarget(Path target) throws BackupException {
        if (Files.exists(target)) {
            throw new BackupException(
                    BackupException.Reason.TARGET_EXISTS,
                    "Целевой файл уже существует; выберите новый путь"
            );
        }
    }

    private static void createDirectories(Path parent, BackupException.Reason reason)
            throws BackupException {
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new BackupException(reason, "Не удалось подготовить целевой каталог", exception);
        }
    }

    private static void moveNewFile(Path source, Path target) throws IOException {
        Files.move(source, target);
    }

    private static void requireSameSnapshot(
            DatabaseInspection expected,
            DatabaseInspection actual
    ) throws BackupException {
        if (expected.schemaVersion() != actual.schemaVersion()
                || expected.planCount() != actual.planCount()) {
            throw new BackupException(
                    BackupException.Reason.INTEGRITY_FAILED,
                    "Проверенная копия не совпадает с ожидаемым состоянием"
            );
        }
    }

    private static void deleteUnpublished(Path staged, Throwable failure) {
        if (staged == null) {
            return;
        }
        try {
            Files.deleteIfExists(staged);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    /** Метаданные опубликованного и повторно открытого снимка. */
    public record BackupSnapshot(
            Path path,
            Instant createdAt,
            DatabaseInspection inspection
    ) {
        public BackupSnapshot {
            path = normalize(path, "Путь снимка обязателен");
            createdAt = Objects.requireNonNull(createdAt, "Время снимка обязательно");
            inspection = Objects.requireNonNull(inspection, "Проверка снимка обязательна");
        }
    }

    /** Результат восстановления в новый файл без изменения исходной копии. */
    public record RestoreResult(
            Path backupPath,
            Path restoredPath,
            DatabaseInspection inspection
    ) {
        public RestoreResult {
            backupPath = normalize(backupPath, "Путь копии обязателен");
            restoredPath = normalize(restoredPath, "Путь восстановления обязателен");
            inspection = Objects.requireNonNull(inspection, "Проверка результата обязательна");
        }
    }

    /** Результат чтения базы отдельным соединением без запуска миграций. */
    public record DatabaseInspection(
            Path databasePath,
            String integrityResult,
            int schemaVersion,
            int planCount
    ) {
        public DatabaseInspection {
            databasePath = normalize(databasePath, "Путь проверяемой базы обязателен");
            integrityResult = Objects.requireNonNull(
                    integrityResult, "Результат integrity_check обязателен");
            if (schemaVersion < 0 || planCount < 0) {
                throw new IllegalArgumentException("Версия и число записей не могут быть отрицательными");
            }
        }
    }
}
