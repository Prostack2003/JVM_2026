package ru.npi.kbju.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Проверяет прикладные сценарии без запуска JavaFX и без знания таблицы. */
class NutritionPlanServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private NutritionPlanService service;

    @BeforeEach
    void setUp() {
        var initial = new NutritionPlan(
                7, "Снижение массы", TODAY, NutritionPlan.Status.DRAFT);
        service = new NutritionPlanService(
                new MemoryNutritionPlanRepository(List.of(initial)),
                Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsPlanThroughRepositoryAndKeepsValues() throws DuplicatePlanException {
        var saved = service.addPlan(
                "  Поддержание массы  ", LocalDate.of(2026, 11, 1),
                NutritionPlan.Status.ACTIVE);

        assertEquals(8, saved.id());
        assertEquals("Поддержание массы", saved.name());
        assertEquals(saved, service.all().getLast());
    }

    @Test
    void duplicateFailureLeavesStateAndNextIdentifierUntouched()
            throws DuplicatePlanException {
        var beforeFailure = service.all();
        assertThrows(DuplicatePlanException.class,
                () -> service.addPlan(" снижение МАССЫ ", TODAY,
                        NutritionPlan.Status.NEEDS_REVIEW));

        assertEquals(beforeFailure, service.all());
        assertEquals(8, service.addDraft("Набор массы").id());
    }

    @Test
    void changesAndRemovesPlanByStableIdentifier() throws DuplicatePlanException {
        var changed = service.changeStatus(7, NutritionPlan.Status.ACTIVE);

        assertEquals(7, changed.id());
        assertEquals(NutritionPlan.Status.ACTIVE, service.all().getFirst().status());
        assertTrue(service.remove(7));
        assertTrue(service.all().isEmpty());
    }

    @Test
    void reportsMissingIdentifierWithoutChangingState() {
        var beforeFailure = service.all();
        assertThrows(NoSuchElementException.class,
                () -> service.changeStatus(999, NutritionPlan.Status.ACTIVE));
        assertEquals(beforeFailure, service.all());
    }
}
