package ru.npi.kbju;

import java.time.LocalDate;
import ru.npi.kbju.domain.NutritionPlan;

/** Демонстрирует создание плана питания и неизменяемую смену его состояния. */
public final class Practice03App {
    private Practice03App() {
    }

    /**
     * Выполняет воспроизводимый маршрут: создать черновик, активировать и сравнить записи.
     *
     * @param args аргументы командной строки; не используются
     */
    public static void main(String[] args) {
        var draftPlan = new NutritionPlan(
                1,
                "Снижение массы",
                LocalDate.of(2026, 10, 1),
                NutritionPlan.Status.DRAFT
        );
        var activePlan = draftPlan.withStatus(NutritionPlan.Status.ACTIVE);

        printPlan("Исходная запись", draftPlan);
        printPlan("Новая запись", activePlan);
        System.out.println("Исходная запись сохранена: "
                + (draftPlan.status() == NutritionPlan.Status.DRAFT));
        System.out.println("Создан новый объект: " + (draftPlan != activePlan));
    }

    private static void printPlan(String label, NutritionPlan plan) {
        System.out.printf(
                "%s: %d | %s | %s | %s%n",
                label,
                plan.id(),
                plan.name(),
                plan.effectiveFrom(),
                plan.status()
        );
    }
}
