package ru.npi.kbju.lesson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.npi.kbju.application.importing.NutritionPlanImportService;
import ru.npi.kbju.domain.BatchImportException;
import ru.npi.kbju.infrastructure.SqliteNutritionPlanRepository;

/** Воспроизводит приёмку транзакций, CSV и повторяемой миграции во временной базе. */
public final class TransactionDemo {
    private static final String RESOURCE_ROOT = "ru/npi/kbju/lesson/import/";

    private TransactionDemo() {
    }

    /** Подтверждает успешный пакет, полный rollback и однократную V002. */
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("kbju-transaction-demo-");
        var validCsv = copyResource("plans-valid.csv", directory);
        var duplicateCsv = copyResource("plans-duplicate-code.csv", directory);
        var database = directory.resolve("nutrition-plans.db");

        try (var repository = new SqliteNutritionPlanRepository(database)) {
            require(repository.schemaVersion() == 2, "Ожидалась схема V002");
            require(repository.appliedMigrationCount() == 2,
                    "Каждая миграция должна быть зарегистрирована один раз");
            var service = new NutritionPlanImportService(repository);

            var summary = service.importCsv(validCsv);
            require(summary.importedCount() == 2, "Корректный пакет сохранён не полностью");
            var restored = repository.findImportedByCode("KBJU-101").orElseThrow();
            require(restored.plan().name().equals("Баланс на неделю"),
                    "Название не совпало после чтения");
            require(restored.plan().effectiveFrom().toString().equals("2026-11-01"),
                    "Дата не совпала после чтения");
            System.out.println("Корректный пакет: сохранены 2 строки со всеми полями");

            var beforeFailure = repository.findAll();
            try {
                service.importCsv(duplicateCsv);
                throw new AssertionError("Дубликат кода должен отменить пакет");
            } catch (BatchImportException expected) {
                require(expected.problemCode().equals("KBJU-201"),
                        "Отказ не указывает проблемный код");
                require(repository.findAll().equals(beforeFailure),
                        "Rollback оставил частичный пакет");
                System.out.println(
                        "Дубликат KBJU-201: весь пакет отменён, строк осталось 2");
            }
        }

        try (var reopened = new SqliteNutritionPlanRepository(database)) {
            require(reopened.schemaVersion() == 2, "Версия схемы изменилась при повторе");
            require(reopened.appliedMigrationCount() == 2,
                    "Повторный запуск продублировал журнал миграций");
            require(reopened.findAll().size() == 2, "Старые данные не сохранились");
            System.out.println(
                    "Повтор мигратора: версия 2, две миграции, старые данные сохранены");
        }
        System.out.println("Файл SQLite и оба CSV находятся во временном учебном каталоге");
    }

    private static Path copyResource(String name, Path directory) throws IOException {
        var resource = RESOURCE_ROOT + name;
        try (var stream = openResource(resource)) {
            if (stream == null) {
                throw new IOException("CSV-ресурс не найден: " + resource);
            }
            var target = directory.resolve(name);
            Files.copy(stream, target);
            return target;
        }
    }

    private static InputStream openResource(String resource) {
        var fromModule = TransactionDemo.class.getResourceAsStream("/" + resource);
        return fromModule != null
                ? fromModule
                : TransactionDemo.class.getClassLoader().getResourceAsStream(resource);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
