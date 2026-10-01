package ru.npi.kbju.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Проверяет инварианты и замену состояния неизменяемого плана питания. */
class NutritionPlanTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    @Test
    void trimsNameAndKeepsValidValues() {
        var plan = new NutritionPlan(1, "  Снижение массы  ", DATE,
                NutritionPlan.Status.DRAFT);

        assertEquals("Снижение массы", plan.name());
        assertEquals(1, plan.id());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                1, "   ", DATE, NutritionPlan.Status.DRAFT));
    }

    @Test
    void rejectsNegativeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                -1, "План", DATE, NutritionPlan.Status.DRAFT));
    }

    @Test
    void requiresDateAndStatus() {
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1, "План", null, NutritionPlan.Status.DRAFT));
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1, "План", DATE, null));
    }

    @Test
    void withStatusReturnsReplacementAndKeepsOriginalUnchanged() {
        var original = new NutritionPlan(1, "План", DATE, NutritionPlan.Status.DRAFT);

        var replacement = original.withStatus(NutritionPlan.Status.ACTIVE);

        assertNotSame(original, replacement);
        assertEquals(NutritionPlan.Status.DRAFT, original.status());
        assertEquals(NutritionPlan.Status.ACTIVE, replacement.status());
        assertEquals(original.id(), replacement.id());
        assertEquals(original.name(), replacement.name());
        assertEquals(original.effectiveFrom(), replacement.effectiveFrom());
    }
}
