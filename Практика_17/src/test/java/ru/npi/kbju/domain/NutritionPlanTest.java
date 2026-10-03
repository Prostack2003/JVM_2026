package ru.npi.kbju.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Проверяет наблюдаемые правила NutritionPlan без JavaFX, часов и общего состояния. */
class NutritionPlanTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    @Test
    void validPlanNormalizesNameAndPreservesEveryComponent() {
        var plan = new NutritionPlan(1, "  Снижение массы  ", DATE,
                NutritionPlan.Status.DRAFT);

        assertAll(
                () -> assertEquals(1, plan.id()),
                () -> assertEquals("Снижение массы", plan.name()),
                () -> assertEquals(DATE, plan.effectiveFrom()),
                () -> assertEquals(NutritionPlan.Status.DRAFT, plan.status()));
    }

    @Test
    void acceptsZeroIdentifierBeforePersistence() {
        var plan = new NutritionPlan(0, "Новый план", DATE,
                NutritionPlan.Status.DRAFT);

        assertEquals(0, plan.id());
    }

    @ParameterizedTest(name = "недопустимое имя [{index}] = <{0}>")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void rejectsEveryBlankNameVariant(String name) {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, name, DATE, NutritionPlan.Status.DRAFT));

        assertEquals(PlanValidationException.Field.NAME, error.field());
    }

    @Test
    void rejectsNameOneCharacterBeyondMaximum() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "А".repeat(NutritionPlan.MAX_NAME_LENGTH + 1), DATE,
                        NutritionPlan.Status.DRAFT));

        assertEquals(PlanValidationException.Field.NAME, error.field());
    }

    @Test
    void acceptsNameAtExactLengthBoundary() {
        var plan = new NutritionPlan(1, "А".repeat(NutritionPlan.MAX_NAME_LENGTH), DATE,
                NutritionPlan.Status.DRAFT);

        assertEquals(NutritionPlan.MAX_NAME_LENGTH, plan.name().length());
    }

    @Test
    void rejectsNegativeIdentifier() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(-1, "План", DATE, NutritionPlan.Status.DRAFT));

        assertEquals(PlanValidationException.Field.ID, error.field());
    }

    @Test
    void rejectsMissingEffectiveDate() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "План", null, NutritionPlan.Status.DRAFT));

        assertEquals(PlanValidationException.Field.EFFECTIVE_FROM, error.field());
    }

    @Test
    void rejectsMissingStatus() {
        var error = assertThrows(PlanValidationException.class,
                () -> new NutritionPlan(1, "План", DATE, null));

        assertEquals(PlanValidationException.Field.STATUS, error.field());
    }

    @Test
    void withStatusCreatesReplacementAndKeepsOriginalRecord() {
        var original = new NutritionPlan(1, "План", DATE, NutritionPlan.Status.DRAFT);

        var replacement = original.withStatus(NutritionPlan.Status.ACTIVE);

        assertAll(
                () -> assertNotSame(original, replacement),
                () -> assertEquals(NutritionPlan.Status.DRAFT, original.status()),
                () -> assertEquals(NutritionPlan.Status.ACTIVE, replacement.status()),
                () -> assertEquals(original.id(), replacement.id()),
                () -> assertEquals(original.name(), replacement.name()),
                () -> assertEquals(original.effectiveFrom(), replacement.effectiveFrom()));
    }

    @Test
    void withStatusRejectsNullInsteadOfCreatingInvalidReplacement() {
        var original = new NutritionPlan(1, "План", DATE, NutritionPlan.Status.DRAFT);

        var error = assertThrows(PlanValidationException.class,
                () -> original.withStatus(null));

        assertAll(
                () -> assertEquals(PlanValidationException.Field.STATUS, error.field()),
                () -> assertEquals(NutritionPlan.Status.DRAFT, original.status()));
    }
}
