package ru.npi.kbju.domain;

import java.time.LocalDate;
import java.util.Locale;

/** Проверенная строка пакетного импорта до открытия транзакции. */
public record NutritionPlanImportRow(String code, String name, LocalDate effectiveFrom) {
    private static final int MAX_CODE_LENGTH = 24;

    /** Нормализует код и повторно использует инварианты плана питания. */
    public NutritionPlanImportRow {
        code = normalizeCode(code);
        var checked = new NutritionPlan(
                0, name, effectiveFrom, NutritionPlan.Status.DRAFT);
        name = checked.name();
        effectiveFrom = checked.effectiveFrom();
    }

    /** Нормализует и проверяет код независимо от остальных полей строки. */
    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Код импорта обязателен");
        }
        var normalized = code.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_CODE_LENGTH
                || !normalized.matches("[A-Z0-9][A-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Код должен содержать 2–24 символа A–Z, 0–9, _ или -");
        }
        return normalized;
    }

    /** Создаёт предметный черновик для сохранения. */
    public NutritionPlan asDraft() {
        return new NutritionPlan(0, name, effectiveFrom, NutritionPlan.Status.DRAFT);
    }
}
