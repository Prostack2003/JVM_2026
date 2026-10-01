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

/** Проверяет операции и уведомления единственного наблюдаемого списка. */
class NutritionPlanServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private NutritionPlan initial;
    private NutritionPlanService service;

    @BeforeEach
    void setUp() {
        initial = new NutritionPlan(1, "Снижение массы", TODAY,
                NutritionPlan.Status.DRAFT);
        var clock = Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);
        service = new NutritionPlanService(List.of(initial), clock);
    }

    @Test
    void startsWithOnePlan() {
        assertEquals(List.of(initial), service.plans());
    }

    @Test
    void addPlanKeepsAllThreeFormValuesAndNotifiesList() {
        var changes = new AtomicInteger();
        service.plans().addListener(
                (ListChangeListener<NutritionPlan>) change -> changes.incrementAndGet());
        var date = LocalDate.of(2026, 11, 1);

        var added = service.addPlan(
                "  Поддержание массы  ", date, NutritionPlan.Status.ACTIVE);

        assertEquals(2, added.id());
        assertEquals("Поддержание массы", added.name());
        assertEquals(date, added.effectiveFrom());
        assertEquals(NutritionPlan.Status.ACTIVE, added.status());
        assertEquals(1, changes.get());
    }

    @Test
    void addDraftUsesClockAndDraftStatus() {
        var added = service.addDraft("Набор массы");
        assertEquals(TODAY, added.effectiveFrom());
        assertEquals(NutritionPlan.Status.DRAFT, added.status());
    }

    @Test
    void duplicateNameDoesNotChangeListOrConsumeIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addPlan(" снижение МАССЫ ", TODAY,
                        NutritionPlan.Status.NEEDS_REVIEW));
        assertEquals(1, service.plans().size());
        assertEquals(2, service.addDraft("Набор массы").id());
    }

    @Test
    void missingDateOrStatusDoesNotChangeList() {
        assertThrows(NullPointerException.class,
                () -> service.addPlan("Без даты", null, NutritionPlan.Status.DRAFT));
        assertThrows(NullPointerException.class,
                () -> service.addPlan("Без состояния", TODAY, null));
        assertEquals(List.of(initial), service.plans());
    }

    @Test
    void blankNameDoesNotChangeList() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addPlan("   ", TODAY, NutritionPlan.Status.DRAFT));
        assertEquals(List.of(initial), service.plans());
    }

    @Test
    void removeNotifiesList() {
        var changes = new AtomicInteger();
        service.plans().addListener(
                (ListChangeListener<NutritionPlan>) change -> changes.incrementAndGet());
        assertTrue(service.remove(initial));
        assertTrue(service.plans().isEmpty());
        assertEquals(1, changes.get());
        assertFalse(service.remove(initial));
    }

    @Test
    void statusChangeReplacesElementAndNotifiesList() {
        var changes = new AtomicInteger();
        service.plans().addListener(
                (ListChangeListener<NutritionPlan>) change -> changes.incrementAndGet());
        var replacement = service.changeStatus(initial, NutritionPlan.Status.ACTIVE);
        assertNotSame(initial, replacement);
        assertEquals(NutritionPlan.Status.DRAFT, initial.status());
        assertEquals(NutritionPlan.Status.ACTIVE, service.plans().getFirst().status());
        assertEquals(1, changes.get());
    }

    @Test
    void statusChangeRejectsPlanOutsideCurrentList() {
        var missing = new NutritionPlan(9, "Другой", TODAY, NutritionPlan.Status.DRAFT);
        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(missing, NutritionPlan.Status.ACTIVE));
        assertEquals(List.of(initial), service.plans());
    }

    @Test
    void publicObservableViewCannotBeMutatedDirectly() {
        assertThrows(UnsupportedOperationException.class, () -> service.plans().clear());
        assertEquals(List.of(initial), service.plans());
    }
}
