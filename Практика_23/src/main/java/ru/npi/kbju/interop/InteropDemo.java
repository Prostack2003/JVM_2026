package ru.npi.kbju.interop;

import java.time.LocalDate;
import ru.npi.kbju.domain.NutritionPlan;

/** Показывает Java-вызов Kotlin-компонента на существующей предметной модели. */
public final class InteropDemo {
    private InteropDemo() {
    }

    /** Запускает положительный сценарий и наблюдаемый отказ null-контракта. */
    public static void main(String[] args) {
        var plan = new NutritionPlan(
                23,
                "Поддержание рациона",
                LocalDate.of(2026, 10, 23),
                NutritionPlan.Status.ACTIVE
        );
        System.out.println(NutritionPlanCaption.format(plan));

        try {
            NutritionPlanCaption.format(null);
            throw new AssertionError("Kotlin-компонент неожиданно принял null");
        } catch (IllegalArgumentException exception) {
            System.out.println("Null-контракт: " + exception.getMessage());
        }
    }
}
