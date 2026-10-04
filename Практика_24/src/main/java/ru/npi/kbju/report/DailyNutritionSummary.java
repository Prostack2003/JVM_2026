package ru.npi.kbju.report;

import java.time.LocalDate;
import java.util.Objects;

/** Неизменяемые данные одного суточного отчёта для поставщиков форматирования. */
public record DailyNutritionSummary(
        LocalDate date,
        int calories,
        int calorieTarget,
        double proteinGrams,
        double fatGrams,
        double carbohydrateGrams
) {
    /** Проверяет данные до передачи доверенному расширению. */
    public DailyNutritionSummary {
        Objects.requireNonNull(date, "Дата отчёта обязательна");
        if (calories < 0 || calorieTarget <= 0) {
            throw new IllegalArgumentException("Калории должны быть неотрицательными, цель — положительной");
        }
        if (!isNonNegativeFinite(proteinGrams)
                || !isNonNegativeFinite(fatGrams)
                || !isNonNegativeFinite(carbohydrateGrams)) {
            throw new IllegalArgumentException("Значения БЖУ должны быть конечными и неотрицательными");
        }
    }

    private static boolean isNonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }
}
