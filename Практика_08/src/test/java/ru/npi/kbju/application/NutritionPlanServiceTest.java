package ru.npi.kbju.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.ListChangeListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.PlanValidationException;

/** Проверяет изменения списка и точные типы ожидаемых отказов. */
class NutritionPlanServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private NutritionPlan initial;
    private NutritionPlanService service;

    @BeforeEach
    void setUp() {
        initial = new NutritionPlan(1, "Снижение массы", TODAY,
                NutritionPlan.Status.DRAFT);
        service = new NutritionPlanService(List.of(initial),
                Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void addPlanKeepsAllValuesAndNotifiesList() throws DuplicatePlanException {
        var changes = new AtomicInteger();
        service.plans().addListener(
                (ListChangeListener<NutritionPlan>) change -> changes.incrementAndGet());
        var added = service.addPlan("  Поддержание массы  ",
                LocalDate.of(2026, 11, 1), NutritionPlan.Status.ACTIVE);
        assertEquals(2, added.id());
        assertEquals("Поддержание массы", added.name());
        assertEquals(1, changes.get());
    }

    @Test
    void checkedDuplicateErrorKeepsListAndIdentifier() throws DuplicatePlanException {
        var error = assertThrows(DuplicatePlanException.class,
                () -> service.addPlan(" снижение МАССЫ ", TODAY,
                        NutritionPlan.Status.NEEDS_REVIEW));
        assertEquals("снижение МАССЫ", error.planName());
        assertEquals(1, service.plans().size());
        assertEquals(2, service.addDraft("Набор массы").id());
    }

    @Test
    void invalidModelValuesNeverReachList() {
        assertThrows(PlanValidationException.class,
                () -> service.addPlan("   ", TODAY, NutritionPlan.Status.DRAFT));
        assertThrows(PlanValidationException.class,
                () -> service.addPlan("План", null, NutritionPlan.Status.DRAFT));
        assertEquals(List.of(initial), service.plans());
    }

    @Test
    void removeAndStatusReplacementStillWork() {
        var replacement = service.changeStatus(initial, NutritionPlan.Status.ACTIVE);
        assertNotSame(initial, replacement);
        assertEquals(NutritionPlan.Status.ACTIVE, service.plans().getFirst().status());
        assertTrue(service.remove(replacement));
        assertTrue(service.plans().isEmpty());
        assertFalse(service.remove(replacement));
    }

    @Test
    void publicObservableViewCannotBeMutatedDirectly() {
        assertThrows(UnsupportedOperationException.class, () -> service.plans().clear());
        assertEquals(List.of(initial), service.plans());
    }
}
