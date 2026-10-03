package ru.npi.kbju.application.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.NutritionPlan;

class ParallelNutritionDiagnosticsTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Test
    void awaitsEveryFutureAndPreservesSnapshotOrder() {
        var plans = plans();
        var barrier = new CyclicBarrier(plans.size());
        var diagnostics = new ParallelNutritionDiagnostics(plan -> {
            barrier.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return result(plan);
        }, TIMEOUT);

        var batch = diagnostics.diagnose(plans);

        assertEquals(3, batch.submittedTasks());
        assertEquals(3, batch.completedTasks());
        assertTrue(batch.peakConcurrency() >= 2);
        assertTrue(batch.executorTerminated());
        assertEquals(List.of(11L, 12L, 13L), batch.diagnostics().stream()
                .map(PlanDiagnostic::planId)
                .toList());
        assertTrue(batch.diagnostics().stream().allMatch(PlanDiagnostic::virtualThread));
    }

    @Test
    void keepsOriginalFailureAsCauseAndCancelsRemainingWork() {
        var sourceFailure = new IllegalStateException("отказ диагностики");
        var diagnostics = new ParallelNutritionDiagnostics(plan -> {
            if (plan.id() == 12) {
                throw sourceFailure;
            }
            return result(plan);
        }, TIMEOUT);

        var exception = assertThrows(
                ParallelDiagnosticException.class,
                () -> diagnostics.diagnose(plans()));

        assertSame(sourceFailure, exception.getCause());
    }

    @Test
    void emptySnapshotIsACompletedBatchAndStillClosesExecutor() {
        var batch = ParallelNutritionDiagnostics.standard(TIMEOUT).diagnose(List.of());

        assertTrue(batch.diagnostics().isEmpty());
        assertEquals(0, batch.submittedTasks());
        assertEquals(0, batch.completedTasks());
        assertEquals(0, batch.peakConcurrency());
        assertTrue(batch.executorTerminated());
    }

    @Test
    void rejectsNullBeforeStartingAnyTask() {
        var diagnostics = ParallelNutritionDiagnostics.standard(TIMEOUT);

        assertThrows(NullPointerException.class, () -> diagnostics.diagnose(null));
        assertThrows(NullPointerException.class,
                () -> diagnostics.diagnose(java.util.Arrays.asList(plans().getFirst(), null)));
    }

    private static List<NutritionPlan> plans() {
        return List.of(
                plan(11, NutritionPlan.Status.ACTIVE),
                plan(12, NutritionPlan.Status.NEEDS_REVIEW),
                plan(13, NutritionPlan.Status.DRAFT));
    }

    private static NutritionPlan plan(long id, NutritionPlan.Status status) {
        return new NutritionPlan(id, "План " + id, LocalDate.of(2026, 10, 3), status);
    }

    private static PlanDiagnostic result(NutritionPlan plan) {
        return new PlanDiagnostic(
                plan.id(), plan.name(), false, "проверен", Thread.currentThread().isVirtual());
    }
}
