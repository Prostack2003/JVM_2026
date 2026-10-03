package ru.npi.kbju.lesson;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import ru.npi.kbju.application.concurrency.DeterministicCounterRace;
import ru.npi.kbju.application.concurrency.ParallelDiagnosticException;
import ru.npi.kbju.application.concurrency.ParallelNutritionDiagnostics;
import ru.npi.kbju.application.concurrency.WaitingTaskCancellation;
import ru.npi.kbju.domain.NutritionPlan;

/** Консольный сценарий приёмки практики 14. */
public final class ConcurrencyDemo {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ConcurrencyDemo() { }

    /** Выполняет гонку, параллельную диагностику, отказ и отмену. */
    public static void main(String[] args) {
        System.out.println("Практика 14: исполнители, виртуальные потоки и гонки данных");
        System.out.println("Прогноз: два обычных increment дадут 1, два атомарных — 2.");

        var race = new DeterministicCounterRace(TIMEOUT).run();
        require(race.unsafeCount() == 1, "Гонка должна воспроизвести потерю обновления");
        require(race.atomicCount() == race.expectedCount(), "AtomicInteger не должен терять обновления");
        require(race.executorTerminated(), "Executor гонки должен завершиться");
        System.out.printf("Гонка: unsafe=%d, atomic=%d, executor closed=%s%n",
                race.unsafeCount(), race.atomicCount(), race.executorTerminated());

        var plans = List.of(
                plan(101, "Поддержание массы", NutritionPlan.Status.ACTIVE),
                plan(102, "Коррекция белка", NutritionPlan.Status.NEEDS_REVIEW),
                plan(103, "Восстановительный день", NutritionPlan.Status.DRAFT));
        var batch = ParallelNutritionDiagnostics.standard(TIMEOUT).diagnose(plans);
        require(batch.completedTasks() == plans.size(), "Нужно дождаться всех Future");
        require(batch.executorTerminated(), "Executor диагностики должен завершиться");
        require(batch.diagnostics().stream().allMatch(result -> result.virtualThread()),
                "Диагностики должны работать в виртуальных потоках");
        System.out.printf("Диагностика: submitted=%d, completed=%d, executor closed=%s%n",
                batch.submittedTasks(), batch.completedTasks(), batch.executorTerminated());
        batch.diagnostics().forEach(result -> System.out.printf("  ID %d: %s — %s%n",
                result.planId(), result.planName(), result.summary()));

        var sourceFailure = new IllegalStateException("учебный отказ диагностики ID 102");
        var failing = new ParallelNutritionDiagnostics(plan -> {
            if (plan.id() == 102) {
                throw sourceFailure;
            }
            return new ru.npi.kbju.application.concurrency.PlanDiagnostic(
                    plan.id(), plan.name(), false, "проверен", Thread.currentThread().isVirtual());
        }, TIMEOUT);
        try {
            failing.diagnose(plans);
            throw new AssertionError("Ожидался контролируемый отказ");
        } catch (ParallelDiagnosticException exception) {
            require(exception.getCause() == sourceFailure, "Исходная ошибка Future должна сохраниться");
            System.out.println("Отказ: причина сохранена — " + exception.getCause().getMessage());
        }

        var cancellation = new WaitingTaskCancellation(TIMEOUT).run();
        require(cancellation.futureCancelled(), "Future должен перейти в состояние cancelled");
        require(cancellation.workerInterrupted(), "Ожидающая задача должна получить interrupt");
        require(cancellation.interruptFlagRestored(), "Флаг interrupt должен быть восстановлен");
        require(cancellation.activeTasksAfterClose() == 0, "После close не должно остаться задач");
        require(cancellation.executorTerminated(), "Executor отмены должен завершиться");
        System.out.printf("Отмена: cancelled=%s, interrupted=%s, active=%d, executor closed=%s%n",
                cancellation.futureCancelled(), cancellation.workerInterrupted(),
                cancellation.activeTasksAfterClose(), cancellation.executorTerminated());

        System.out.println("Граница: JDBC читает планы до запуска; задачи делят только immutable snapshot.");
        System.out.println("Итог: все сценарии практики 14 подтверждены.");
    }

    private static NutritionPlan plan(long id, String name, NutritionPlan.Status status) {
        return new NutritionPlan(id, name, LocalDate.of(2026, 10, 3), status);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
