package ru.npi.kbju.report;

import java.util.Locale;
import java.util.Objects;

/** Человекочитаемый поставщик полного текстового отчёта. */
public final class PlainNutritionReport implements NutritionReportPlugin {
    /** Публичный конструктор без аргументов нужен ServiceLoader. */
    public PlainNutritionReport() {
    }

    @Override
    public String id() {
        return "plain";
    }

    @Override
    public String format(DailyNutritionSummary summary) {
        Objects.requireNonNull(summary, "Сводка обязательна");
        return String.format(Locale.ROOT,
                "Отчёт за %s: %d/%d ккал; белки %.1f г; жиры %.1f г; углеводы %.1f г.",
                summary.date(), summary.calories(), summary.calorieTarget(),
                summary.proteinGrams(), summary.fatGrams(), summary.carbohydrateGrams());
    }
}
