package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npi.kbju.domain.BatchImportException;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanImportRow;

/** Проверяет JDBC-адаптер на временных файлах, не затрагивая пользовательскую базу. */
class SqliteNutritionPlanRepositoryTest {
    @TempDir
    Path directory;

    @Test
    void insertAndReadPreserveEveryFieldIncludingApostrophe() throws Exception {
        var database = directory.resolve("insert-read.db");
        var source = new NutritionPlan(
                0, "План О'Нила", LocalDate.of(2026, 11, 15),
                NutritionPlan.Status.NEEDS_REVIEW);

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            var saved = repository.save(source);
            var restored = repository.findById(saved.id()).orElseThrow();

            assertTrue(saved.id() > 0);
            assertEquals(saved, restored);
            assertEquals("План О'Нила", restored.name());
        }
    }

    @Test
    void recordSurvivesClosingAndReopeningDatabase() throws Exception {
        var database = directory.resolve("reopen.db");
        final NutritionPlan saved;
        try (var first = new SqliteNutritionPlanRepository(database)) {
            saved = first.save(plan("Поддержание массы"));
        }

        try (var second = new SqliteNutritionPlanRepository(database)) {
            assertEquals(saved, second.findById(saved.id()).orElseThrow());
            assertEquals(1, second.findAll().size());
        }
    }

    @Test
    void normalizedDuplicateBecomesDomainFailureWithoutChangingRows() throws Exception {
        try (var repository = new SqliteNutritionPlanRepository(
                directory.resolve("duplicate.db"))) {
            repository.save(plan("Снижение массы"));
            var before = repository.findAll();

            var failure = assertThrowsExactly(DuplicatePlanException.class,
                    () -> repository.save(plan("  снижение МАССЫ  ")));

            assertEquals("снижение МАССЫ", failure.planName());
            assertEquals(before, repository.findAll());
        }
    }

    @Test
    void updateAndDeleteUseStableIdentifier() throws Exception {
        try (var repository = new SqliteNutritionPlanRepository(
                directory.resolve("update-delete.db"))) {
            var saved = repository.save(plan("Набор массы"));
            var changed = saved.withStatus(NutritionPlan.Status.ACTIVE);

            assertEquals(changed, repository.save(changed));
            assertEquals(changed, repository.findById(saved.id()).orElseThrow());
            assertTrue(repository.deleteById(saved.id()));
            assertFalse(repository.deleteById(saved.id()));
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Test
    void updateOfUnknownIdentifierIsDiagnosable() {
        try (var repository = new SqliteNutritionPlanRepository(
                directory.resolve("missing.db"))) {
            var unknown = new NutritionPlan(
                    404, "Несуществующий план", LocalDate.of(2026, 11, 15),
                    NutritionPlan.Status.DRAFT);
            assertThrowsExactly(NoSuchElementException.class,
                    () -> repository.save(unknown));
        }
    }

    @Test
    void databaseConstraintsProtectAlternateWritePath() throws Exception {
        var database = directory.resolve("constraints.db");
        try (var ignored = new SqliteNutritionPlanRepository(database)) {
            assertTrue(ignored.findAll().isEmpty());
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.prepareStatement(
                     "INSERT INTO nutrition_plan"
                             + "(name, name_key, effective_from, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, "План с неверным состоянием");
            statement.setString(2, "план с неверным состоянием");
            statement.setString(3, "2026-11-15");
            statement.setString(4, "UNKNOWN");
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }

    @Test
    void corruptedDateIsReportedInsteadOfBeingSilentlyReplaced() throws Exception {
        var database = directory.resolve("corrupted-date.db");
        try (var ignored = new SqliteNutritionPlanRepository(database)) {
            assertTrue(ignored.findAll().isEmpty());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.prepareStatement(
                     "INSERT INTO nutrition_plan"
                             + "(name, name_key, effective_from, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, "Повреждённая дата");
            statement.setString(2, "повреждённая дата");
            statement.setString(3, "2026-99-99");
            statement.setString(4, "DRAFT");
            assertEquals(1, statement.executeUpdate());
        }

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            var failure = assertThrowsExactly(IllegalStateException.class, repository::findAll);
            assertTrue(failure.getMessage().contains("несовместима с моделью"));
            assertInstanceOf(java.time.format.DateTimeParseException.class, failure.getCause());
        }
    }

    @Test
    void batchImportCommitsAllFieldsAndCodesTogether() throws Exception {
        var database = directory.resolve("batch-success.db");
        try (var repository = new SqliteNutritionPlanRepository(database)) {
            var imported = repository.importBatch(List.of(
                    importRow("KBJU-501", "Баланс на неделю", 1),
                    importRow("KBJU-502", "Поддержание массы", 2)));

            assertEquals(2, imported.size());
            assertEquals(2, repository.findAll().size());
            var restored = repository.findImportedByCode(" kbju-501 ").orElseThrow();
            assertEquals("KBJU-501", restored.code());
            assertEquals("Баланс на неделю", restored.plan().name());
            assertEquals(LocalDate.of(2026, 11, 1), restored.plan().effectiveFrom());
            assertEquals(NutritionPlan.Status.DRAFT, restored.plan().status());
        }

        try (var reopened = new SqliteNutritionPlanRepository(database)) {
            assertEquals("Поддержание массы",
                    reopened.findImportedByCode("KBJU-502").orElseThrow().plan().name());
        }
    }

    @Test
    void duplicateCodeRollsBackWholePackageAndNamesProblemCode() throws Exception {
        try (var repository = new SqliteNutritionPlanRepository(
                directory.resolve("duplicate-code.db"))) {
            repository.save(plan("Ранее сохранённый план"));
            var before = repository.findAll();

            var failure = assertThrowsExactly(BatchImportException.class,
                    () -> repository.importBatch(List.of(
                            importRow("KBJU-601", "Первый новый план", 1),
                            importRow("KBJU-601", "Второй новый план", 2))));

            assertEquals("KBJU-601", failure.problemCode());
            assertTrue(failure.getMessage().contains("KBJU-601"));
            assertEquals(before, repository.findAll());
            assertTrue(repository.findImportedByCode("KBJU-601").isEmpty());
        }
    }

    @Test
    void duplicateNormalizedNameAlsoRollsBackRowsWithDifferentCodes() throws Exception {
        try (var repository = new SqliteNutritionPlanRepository(
                directory.resolve("duplicate-name.db"))) {
            var before = repository.findAll();

            var failure = assertThrowsExactly(BatchImportException.class,
                    () -> repository.importBatch(List.of(
                            importRow("KBJU-701", "Контроль массы", 1),
                            importRow("KBJU-702", "  контроль МАССЫ  ", 2))));

            assertEquals("KBJU-702", failure.problemCode());
            assertEquals(before, repository.findAll());
            assertTrue(repository.findImportedByCode("KBJU-701").isEmpty());
        }
    }

    private static NutritionPlan plan(String name) {
        return new NutritionPlan(
                0, name, LocalDate.of(2026, 11, 15), NutritionPlan.Status.DRAFT);
    }

    private static NutritionPlanImportRow importRow(
            String code,
            String name,
            int day
    ) {
        return new NutritionPlanImportRow(
                code, name, LocalDate.of(2026, 11, day));
    }
}
