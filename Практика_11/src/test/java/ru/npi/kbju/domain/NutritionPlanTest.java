package ru.npi.kbju.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Проверяет два перенесённых в предметную модель ограничения и остальные инварианты. */
class NutritionPlanTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    @Test
    void trimsNameAndKeepsValidValues() {
        var plan = new NutritionPlan(1, "  Снижение массы  ", DATE,
                NutritionPlan.Status.DRAFT);
        assertEquals("Снижение массы", plan.name());
    }

    @Test
    void modelRejectsTheSameBlankNameAsTheForm() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "   ", DATE, NutritionPlan.Status.DRAFT));
        assertEquals(PlanValidationException.Field.NAME, error.field());
    }

    @Test
    void modelRejectsTheSameLongNameAsTheForm() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "А".repeat(61), DATE,
                        NutritionPlan.Status.DRAFT));
        assertEquals(PlanValidationException.Field.NAME, error.field());
    }

    @Test
    void acceptsNameAtExactLengthBoundary() {
        var plan = new NutritionPlan(1, "А".repeat(60), DATE,
                NutritionPlan.Status.DRAFT);
        assertEquals(60, plan.name().length());
    }

    @Test
    void reportsMissingDateAndStatusByField() {
        var dateError = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "План", null, NutritionPlan.Status.DRAFT));
        var statusError = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "План", DATE, null));
        assertEquals(PlanValidationException.Field.EFFECTIVE_FROM, dateError.field());
        assertEquals(PlanValidationException.Field.STATUS, statusError.field());
    }

    @Test
    void rejectsNegativeIdentifier() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(-1, "План", DATE, NutritionPlan.Status.DRAFT));
        assertEquals(PlanValidationException.Field.ID, error.field());
    }

    @Test
    void withStatusReturnsReplacementAndKeepsOriginalUnchanged() {
        var original = new NutritionPlan(1, "План", DATE, NutritionPlan.Status.DRAFT);
        var replacement = original.withStatus(NutritionPlan.Status.ACTIVE);
        assertNotSame(original, replacement);
        assertEquals(NutritionPlan.Status.DRAFT, original.status());
        assertEquals(NutritionPlan.Status.ACTIVE, replacement.status());
    }
}
