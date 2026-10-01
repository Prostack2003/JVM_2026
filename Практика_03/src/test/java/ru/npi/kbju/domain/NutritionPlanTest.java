package ru.npi.kbju.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Проверяет допустимые и запрещённые состояния плана питания. */
class NutritionPlanTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

    /** Подтверждает создание корректной записи и явно принятое удаление крайних пробелов. */
    @Test
    @DisplayName("Корректные поля создают план и нормализуют название")
    void createsValidPlanAndTrimsName() {
        var plan = new NutritionPlan(
                1,
                "  Снижение массы  ",
                START_DATE,
                NutritionPlan.Status.DRAFT
        );

        assertEquals(1, plan.id());
        assertEquals("Снижение массы", plan.name());
        assertEquals(START_DATE, plan.effectiveFrom());
        assertEquals(NutritionPlan.Status.DRAFT, plan.status());
    }

    /** Подтверждает, что ноль обозначает новую запись до назначения постоянного id. */
    @Test
    @DisplayName("Нулевой идентификатор допустим до сохранения")
    void allowsZeroIdentifierForNewPlan() {
        var plan = new NutritionPlan(
                0,
                "Поддержание массы",
                START_DATE,
                NutritionPlan.Status.DRAFT
        );

        assertEquals(0, plan.id());
    }

    /** Проверяет запрет отрицательного идентификатора. */
    @Test
    @DisplayName("Отрицательный идентификатор отклоняется")
    void rejectsNegativeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                -1,
                "Поддержание массы",
                START_DATE,
                NutritionPlan.Status.DRAFT
        ));
    }

    /** Проверяет запрет пустого или пробельного названия. */
    @Test
    @DisplayName("Пустое название отклоняется")
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                1,
                "   ",
                START_DATE,
                NutritionPlan.Status.DRAFT
        ));
    }

    /** Проверяет запрет отсутствующего названия. */
    @Test
    @DisplayName("Отсутствующее название отклоняется")
    void rejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionPlan(
                1,
                null,
                START_DATE,
                NutritionPlan.Status.DRAFT
        ));
    }

    /** Проверяет обязательность даты начала. */
    @Test
    @DisplayName("Отсутствующая дата отклоняется")
    void rejectsMissingDate() {
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1,
                "Поддержание массы",
                null,
                NutritionPlan.Status.DRAFT
        ));
    }

    /** Проверяет обязательность состояния. */
    @Test
    @DisplayName("Отсутствующее состояние отклоняется")
    void rejectsMissingStatus() {
        assertThrows(NullPointerException.class, () -> new NutritionPlan(
                1,
                "Поддержание массы",
                START_DATE,
                null
        ));
    }

    /** Подтверждает создание новой записи без изменения исходного плана. */
    @Test
    @DisplayName("Смена состояния создаёт новую запись")
    void changingStatusCreatesNewRecordAndPreservesSource() {
        var draftPlan = new NutritionPlan(
                1,
                "Снижение массы",
                START_DATE,
                NutritionPlan.Status.DRAFT
        );

        var activePlan = draftPlan.withStatus(NutritionPlan.Status.ACTIVE);

        assertNotSame(draftPlan, activePlan);
        assertNotEquals(draftPlan, activePlan);
        assertEquals(NutritionPlan.Status.DRAFT, draftPlan.status());
        assertEquals(NutritionPlan.Status.ACTIVE, activePlan.status());
        assertEquals(draftPlan.id(), activePlan.id());
        assertEquals(draftPlan.name(), activePlan.name());
        assertEquals(draftPlan.effectiveFrom(), activePlan.effectiveFrom());
    }
}
