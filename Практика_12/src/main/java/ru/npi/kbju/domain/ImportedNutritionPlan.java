package ru.npi.kbju.domain;

import java.util.Objects;

/** Сохранённый план вместе с кодом исходной строки импорта. */
public record ImportedNutritionPlan(String code, NutritionPlan plan) {
    /** Проверяет обязательность обеих частей результата. */
    public ImportedNutritionPlan {
        code = Objects.requireNonNull(code, "Код импорта обязателен");
        plan = Objects.requireNonNull(plan, "Сохранённый план обязателен");
    }
}
