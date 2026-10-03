package ru.npi.kbju.application.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.PlanValidationException;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Проверяет прозрачность proxy и границы счётчика предметных вызовов. */
class ObservedNutritionPlanServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate START = LocalDate.of(2026, 10, 2);

    @Test
    void proxyKeepsResultsAndCountsOnlyAnnotatedCommands() throws Exception {
        NutritionPlanOperations direct = new NutritionPlanService(
                new MemoryNutritionPlanRepository(), CLOCK);
        var metrics = new OperationCallMetrics();
        NutritionPlanOperations observed = ObservedNutritionPlanService.create(
                new NutritionPlanService(new MemoryNutritionPlanRepository(), CLOCK), metrics);

        var directResult = direct.addPlan("Поддержание массы", START,
                NutritionPlan.Status.DRAFT);
        var proxyResult = observed.addPlan("Поддержание массы", START,
                NutritionPlan.Status.DRAFT);
        var changed = observed.changeStatus(proxyResult.id(), NutritionPlan.Status.ACTIVE);
        observed.all();

        assertEquals(directResult, proxyResult);
        assertEquals(NutritionPlan.Status.ACTIVE, changed.status());
        assertEquals(2, metrics.total());
        assertEquals(1, metrics.count("addPlan"));
        assertEquals(1, metrics.count("changeStatus"));
        assertEquals(List.of("addPlan", "changeStatus"),
                metrics.snapshot().keySet().stream().toList());
    }

    @Test
    void objectMethodsAreExplicitAndDoNotAffectMetrics() {
        var target = new NutritionPlanService(new MemoryNutritionPlanRepository(), CLOCK);
        var metrics = new OperationCallMetrics();
        var observed = ObservedNutritionPlanService.create(target, metrics);

        assertTrue(observed.equals(observed));
        assertFalse(observed.equals(target));
        assertEquals(System.identityHashCode(observed), observed.hashCode());
        assertEquals("Наблюдаемый сервис планов питания", observed.toString());
        assertEquals(0, metrics.total());
    }

    @Test
    void uncheckedDomainFailureIsNotHiddenByReflectionWrapper() {
        var metrics = new OperationCallMetrics();
        var observed = ObservedNutritionPlanService.create(
                new NutritionPlanService(new MemoryNutritionPlanRepository(), CLOCK), metrics);

        var failure = assertThrowsExactly(PlanValidationException.class,
                () -> observed.addPlan("   ", START, NutritionPlan.Status.DRAFT));

        assertEquals(PlanValidationException.Field.NAME, failure.field());
        assertEquals(1, metrics.total());
    }

    @Test
    void checkedDuplicateFailureKeepsItsExactTypeAndInstance() throws Exception {
        var repository = new MemoryNutritionPlanRepository();
        var target = new NutritionPlanService(repository, CLOCK);
        var metrics = new OperationCallMetrics();
        var observed = ObservedNutritionPlanService.create(target, metrics);
        observed.addPlan("Снижение массы", START, NutritionPlan.Status.DRAFT);

        var failure = assertThrowsExactly(DuplicatePlanException.class,
                () -> observed.addPlan(" снижение МАССЫ ", START,
                        NutritionPlan.Status.ACTIVE));

        assertEquals("снижение МАССЫ", failure.planName());
        assertEquals(2, metrics.total());
    }

    @Test
    void factoryRejectsMissingDependencies() {
        var metrics = new OperationCallMetrics();
        var target = new NutritionPlanService(new MemoryNutritionPlanRepository(), CLOCK);

        assertThrowsExactly(NullPointerException.class,
                () -> ObservedNutritionPlanService.create(null, metrics));
        assertThrowsExactly(NullPointerException.class,
                () -> ObservedNutritionPlanService.create(target, null));
        assertNotEquals(target, ObservedNutritionPlanService.create(target, metrics));
    }
}
