package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npi.kbju.domain.NutritionPlan;

/** Проверяет пригодность снимка и неизменность защищённых данных при отказе. */
class SqliteBackupServiceTest {
    private static final Instant FIXED_TIME = Instant.parse("2026-10-03T12:00:00Z");

    @TempDir
    Path directory;

    @Test
    void liveDatabaseSnapshotOpensSeparatelyAndContainsExpectedRecords() throws Exception {
        var database = directory.resolve("working/nutrition.db");
        var target = directory.resolve("backup/nutrition.db");
        var service = service();
        final List<NutritionPlan> expected;

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            repository.save(plan("Контроль массы", 1));
            repository.save(plan("Баланс КБЖУ", 2));
            expected = repository.findAll();

            var snapshot = service.create(database, target);

            assertEquals(FIXED_TIME, snapshot.createdAt());
            assertEquals("ok", snapshot.inspection().integrityResult());
            assertEquals(2, snapshot.inspection().schemaVersion());
            assertEquals(expected.size(), snapshot.inspection().planCount());
        }

        try (var backup = new SqliteNutritionPlanRepository(target)) {
            assertEquals(expected, backup.findAll());
        }
    }

    @Test
    void laterChangeDoesNotAppearInOlderSnapshot() throws Exception {
        var database = databaseWith("Состояние снимка");
        var target = directory.resolve("backup/older.db");
        service().create(database, target);

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            repository.save(plan("Позднее изменение", 3));
            assertEquals(2, repository.findAll().size());
        }
        try (var backup = new SqliteNutritionPlanRepository(target)) {
            assertEquals(List.of("Состояние снимка"),
                    backup.findAll().stream().map(NutritionPlan::name).toList());
        }
    }

    @Test
    void restoresOnlyToNewPathAndPreservesEveryField() throws Exception {
        var database = databaseWith("Восстанавливаемый план");
        var backup = directory.resolve("backup/good.db");
        var target = directory.resolve("restore/new.db");
        var service = service();
        service.create(database, backup);

        var result = service.restoreToNewDatabase(backup, target);

        assertEquals(target.toAbsolutePath(), result.restoredPath());
        assertEquals("ok", result.inspection().integrityResult());
        try (var source = new SqliteNutritionPlanRepository(database);
             var restored = new SqliteNutritionPlanRepository(target)) {
            assertEquals(source.findAll(), restored.findAll());
        }
    }

    @Test
    void damagedFileIsRejectedWithoutChangingWorkingDatabaseOrGoodSnapshot() throws Exception {
        var database = databaseWith("Защищённая рабочая запись");
        var goodBackup = directory.resolve("backup/good.db");
        var service = service();
        service.create(database, goodBackup);
        var sourceHash = sha256(database);
        var backupHash = sha256(goodBackup);
        var damaged = directory.resolve("damaged.db");
        Files.writeString(damaged, "not sqlite", StandardCharsets.UTF_8);
        var target = directory.resolve("restore/rejected.db");

        var failure = assertThrowsExactly(BackupException.class,
                () -> service.restoreToNewDatabase(damaged, target));

        assertEquals(BackupException.Reason.INTEGRITY_FAILED, failure.reason());
        assertEquals(sourceHash, sha256(database));
        assertEquals(backupHash, sha256(goodBackup));
        assertFalse(Files.exists(target));
    }

    @Test
    void existingRestoreTargetIsRejectedAndItsBytesStayUntouched() throws Exception {
        var database = databaseWith("Источник восстановления");
        var backup = directory.resolve("backup/good.db");
        var target = directory.resolve("restore/existing.db");
        var service = service();
        service.create(database, backup);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "keep me", StandardCharsets.UTF_8);

        var failure = assertThrowsExactly(BackupException.class,
                () -> service.restoreToNewDatabase(backup, target));

        assertEquals(BackupException.Reason.TARGET_EXISTS, failure.reason());
        assertEquals("keep me", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void repeatedBackupTargetIsRejectedWithoutOverwritingFirstSnapshot() throws Exception {
        var database = databaseWith("Первая версия");
        var target = directory.resolve("backup/one.db");
        var service = service();
        service.create(database, target);
        var firstHash = sha256(target);

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            repository.save(plan("Вторая версия", 2));
        }
        var failure = assertThrowsExactly(BackupException.class,
                () -> service.create(database, target));

        assertEquals(BackupException.Reason.TARGET_EXISTS, failure.reason());
        assertEquals(firstHash, sha256(target));
    }

    @Test
    void schemaWithoutMigrationHistoryIsRejectedBeforeTargetCreation() throws Exception {
        var incompatible = directory.resolve("incompatible.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + incompatible);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE foreign_data(value TEXT)");
        }
        var target = directory.resolve("restore/unsupported.db");

        var failure = assertThrowsExactly(BackupException.class,
                () -> service().restoreToNewDatabase(incompatible, target));

        assertEquals(BackupException.Reason.UNSUPPORTED_SCHEMA, failure.reason());
        assertFalse(Files.exists(target));
    }

    @Test
    void sourceAndTargetMustDifferEvenWhenBackupIsValid() throws Exception {
        var database = databaseWith("Нельзя перезаписать себя");

        var failure = assertThrowsExactly(BackupException.class,
                () -> service().restoreToNewDatabase(database, database));

        assertEquals(BackupException.Reason.SAME_PATH, failure.reason());
        assertTrue(Files.isRegularFile(database));
    }

    @Test
    void sqliteSnapshotEscapesApostropheInTargetPath() throws Exception {
        var database = databaseWith("Путь с апострофом");
        var target = directory.resolve("О'Нил/backup.db");

        var snapshot = service().create(database, target);

        assertEquals("ok", snapshot.inspection().integrityResult());
        assertTrue(Files.isRegularFile(target));
    }

    private Path databaseWith(String name) throws Exception {
        var database = directory.resolve("data-" + Math.abs(name.hashCode()) + ".db");
        try (var repository = new SqliteNutritionPlanRepository(database)) {
            repository.save(plan(name, 1));
        }
        return database;
    }

    private static NutritionPlan plan(String name, int day) {
        return new NutritionPlan(
                0,
                name,
                LocalDate.of(2026, 10, day),
                NutritionPlan.Status.DRAFT
        );
    }

    private static SqliteBackupService service() {
        return new SqliteBackupService(Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
    }

    private static String sha256(Path path) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
    }
}
