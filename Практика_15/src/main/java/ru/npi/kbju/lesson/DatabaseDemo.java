package ru.npi.kbju.lesson;

import java.nio.file.Files;
import java.time.LocalDate;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.infrastructure.SqliteNutritionPlanRepository;

/** Выполняет четыре воспроизводимых сценария приёмки SQLite практики 11. */
public final class DatabaseDemo {
    private DatabaseDemo() {
    }

    /** Проверяет полное отображение, повторное открытие, апостроф и уникальность. */
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("kbju-database-demo-");
        var database = directory.resolve("nutrition-plans.db");
        var sample = new NutritionPlan(
                0, "План О'Нила", LocalDate.of(2026, 11, 15),
                NutritionPlan.Status.NEEDS_REVIEW);

        final NutritionPlan saved;
        try (var first = new SqliteNutritionPlanRepository(database)) {
            saved = first.save(sample);
            var restored = first.findById(saved.id()).orElseThrow();
            require(saved.equals(restored), "Поля после вставки и чтения не совпали");
            require(restored.name().contains("'"), "Апостроф потерян при записи");
            System.out.println("Вставка и чтение: все поля совпали");
            System.out.println("Апостроф в названии: текст сохранён целиком");
        }

        try (var reopened = new SqliteNutritionPlanRepository(database)) {
            require(reopened.findById(saved.id()).orElseThrow().equals(saved),
                    "Запись исчезла после повторного открытия файла");
            System.out.println("Повторное открытие: запись сохранена");

            var beforeDuplicate = reopened.findAll();
            try {
                reopened.save(new NutritionPlan(
                        0, "  план о'нила  ", LocalDate.of(2026, 12, 1),
                        NutritionPlan.Status.DRAFT));
                throw new AssertionError("Нормализованный дубликат должен быть отклонён");
            } catch (DuplicatePlanException expected) {
                require(beforeDuplicate.equals(reopened.findAll()),
                        "Отказ дубликата изменил сохранённые данные");
                System.out.println("Дубликат названия: получен понятный предметный отказ");
            }
        }

        System.out.println("Файл SQLite: nutrition-plans.db во временном каталоге");
        System.out.println("Пользовательские значения переданы через параметры PreparedStatement");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
