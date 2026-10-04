package ru.npi.kbju.report;

import java.util.Locale;
import java.util.Objects;

/** Компактный поставщик однострочного отчёта для быстрого просмотра. */
public final class CompactNutritionReport implements NutritionReportPlugin {
    /** Публичный конструктор без аргументов нужен ServiceLoader. */
    public CompactNutritionReport() {
    }

    @Override
    public String id() {
        return "compact";
    }

    @Override
    public String format(DailyNutritionSummary summary) {
        Objects.requireNonNull(summary, "Сводка обязательна");
        return String.format(Locale.ROOT,
                "%s | %d/%d ккал | Б %.1f | Ж %.1f | У %.1f",
                summary.date(), summary.calories(), summary.calorieTarget(),
                summary.proteinGrams(), summary.fatGrams(), summary.carbohydrateGrams());
    }
}
