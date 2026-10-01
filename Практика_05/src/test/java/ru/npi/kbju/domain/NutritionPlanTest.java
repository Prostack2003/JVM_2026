package ru.npi.kbju.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Проверяет перенесённые из практики 03 инварианты плана питания. */
class NutritionPlanTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

    @Test
    void createsValidPlanAndTrimsName() {
        var plan = new NutritionPlan(
                1, "  Снижение массы  ", START_DATE, NutritionPlan.Status.DRAFT);

        assertEquals("Снижение массы", plan.name());
        assertEquals(START_DATE, plan.effectiveFrom());
    }

    @Test
    void allowsZeroIdentifierForUnsavedPlan() {
        var plan = new NutritionPlan(
                0, "Поддержание массы", START_DATE, NutritionPlan.Status.DRAFT);

        assertEquals(0, plan.id());
    }

    @Test
    void rejectsNegativeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                -1, "Поддержание массы", START_DATE, NutritionPlan.Status.DRAFT));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                1, "   ", START_DATE, NutritionPlan.Status.DRAFT));
    }

    @Test
    void rejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                1, null, START_DATE, NutritionPlan.Status.DRAFT));
    }

    @Test
    void rejectsMissingDate() {
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1, "Поддержание массы", null, NutritionPlan.Status.DRAFT));
    }

    @Test
    void rejectsMissingStatus() {
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1, "Поддержание массы", START_DATE, null));
    }

    @Test
    void changingStatusCreatesNewRecordAndPreservesSource() {
        var source = new NutritionPlan(
                1, "Снижение массы", START_DATE, NutritionPlan.Status.DRAFT);

        var changed = source.withStatus(NutritionPlan.Status.ACTIVE);

        assertNotSame(source, changed);
        assertEquals(NutritionPlan.Status.DRAFT, source.status());
        assertEquals(NutritionPlan.Status.ACTIVE, changed.status());
    }
}
