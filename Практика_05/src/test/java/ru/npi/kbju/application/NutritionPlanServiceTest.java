package ru.npi.kbju.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.NutritionPlan;

/** Проверяет обычный, граничный и ошибочный сценарии операции добавления. */
class NutritionPlanServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private NutritionPlanService service;

    @BeforeEach
    void setUp() {
        var initial = new NutritionPlan(
                1, "Снижение массы", TODAY, NutritionPlan.Status.DRAFT);
        var clock = Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);
        service = new NutritionPlanService(List.of(initial), clock);
    }

    @Test
    void startsWithOnePlan() {
        assertEquals(1, service.plans().size());
    }

    @Test
    void addsExactlyOneDraftWithNextIdentifier() {
        var added = service.addDraft("  Поддержание массы  ");

        assertEquals(2, service.plans().size());
        assertEquals(2, added.id());
        assertEquals("Поддержание массы", added.name());
        assertEquals(TODAY, added.effectiveFrom());
        assertEquals(NutritionPlan.Status.DRAFT, added.status());
    }

    @Test
    void blankNameDoesNotChangePlansOrConsumeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> service.addDraft("   "));
        assertEquals(1, service.plans().size());

        var added = service.addDraft("Набор массы");
        assertEquals(2, added.id());
    }

    @Test
    void returnedSnapshotCannotChangeServiceState() {
        var snapshot = service.plans();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        assertEquals(1, service.plans().size());
    }

    @Test
    void emptyInitialCollectionStartsIdentifiersFromOne() {
        var clock = Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);
        var emptyService = new NutritionPlanService(List.of(), clock);

        assertEquals(1, emptyService.addDraft("Первый план").id());
    }
}
