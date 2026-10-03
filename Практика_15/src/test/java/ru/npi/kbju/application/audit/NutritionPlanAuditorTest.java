package ru.npi.kbju.application.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.NutritionPlan;

/** Проверяет вычислительное ядро аудита без JavaFX и реального ожидания. */
class NutritionPlanAuditorTest {
    private static final NutritionPlan FIRST = plan(
            1, "Баланс", NutritionPlan.Status.ACTIVE);
    private static final NutritionPlan SECOND = plan(
            2, "Коррекция", NutritionPlan.Status.NEEDS_REVIEW);

    @Test
    void publishesResultOnlyAfterEveryCheckCompletes() throws Exception {
        var progress = new ArrayList<AuditProgress>();
        var result = immediateAuditor().audit(
                List.of(FIRST, SECOND), () -> false, progress::add);

        assertEquals(2, result.checkedPlans());
        assertEquals(1, result.activePlans());
        assertEquals(1, result.plansNeedingReview());
        assertEquals(12, result.completedChecks());
        assertEquals(12, progress.size());
        assertEquals(12, progress.getLast().completed());
        assertEquals("\u0443\u043d\u0438\u043a\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c ID", progress.getLast().checkName());
    }

    @Test
    void cancellationStopsAtBoundaryAndReturnsNoPartialResult() {
        var cancellation = new AtomicBoolean();
        var completed = new ArrayList<Integer>();

        var exception = assertThrows(CancellationException.class, () -> immediateAuditor().audit(
                List.of(FIRST, SECOND), cancellation::get, progress -> {
                    completed.add(progress.completed());
                    if (progress.completed() == 2) {
                        cancellation.set(true);
                    }
                }));

        assertEquals(List.of(1, 2), completed);
        assertTrue(exception.getMessage().contains("безопасной точке"));
    }

    @Test
    void duplicateStableIdFailsInsteadOfPublishingSummary() {
        var duplicate = plan(1, "Другой план", NutritionPlan.Status.DRAFT);

        var exception = assertThrows(AuditExecutionException.class, () -> immediateAuditor().audit(
                List.of(FIRST, duplicate), () -> false, ignored -> { }));

        assertEquals("Обнаружен повтор ID: 1", exception.getMessage());
    }

    @Test
    void inputIsCopiedBeforeLongWorkStarts() throws Exception {
        var mutableInput = new ArrayList<>(List.of(FIRST, SECOND));
        var firstPause = new AtomicBoolean(true);
        var auditor = new NutritionPlanAuditor(Duration.ZERO, ignored -> {
            if (firstPause.getAndSet(false)) {
                mutableInput.clear();
            }
        });

        var result = auditor.audit(mutableInput, () -> false, ignored -> { });

        assertEquals(2, result.checkedPlans());
        assertTrue(mutableInput.isEmpty());
    }

    @Test
    void interruptionFromBlockingStepIsNotHidden() {
        var auditor = new NutritionPlanAuditor(Duration.ZERO, ignored -> {
            throw new InterruptedException("остановка ожидания");
        });

        var exception = assertThrows(InterruptedException.class, () -> auditor.audit(
                List.of(FIRST), () -> false, ignored -> { }));

        assertEquals("остановка ожидания", exception.getMessage());
    }

    private static NutritionPlanAuditor immediateAuditor() {
        return new NutritionPlanAuditor(Duration.ZERO, ignored -> { });
    }

    private static NutritionPlan plan(long id, String name, NutritionPlan.Status status) {
        return new NutritionPlan(id, name, LocalDate.of(2026, 10, 1), status);
    }
}
