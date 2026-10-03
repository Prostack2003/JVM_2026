package ru.npi.kbju.application.concurrency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет независимые чтения неизменяемого снимка планов в виртуальных потоках. */
public final class ParallelNutritionDiagnostics {
    /** Одна независимая диагностическая операция. */
    @FunctionalInterface
    public interface Operation {
        /** Анализирует один план; может завершиться предметной ошибкой. */
        PlanDiagnostic diagnose(NutritionPlan plan) throws Exception;
    }

    private final Operation operation;
    private final Duration timeout;

    /** Принимает чистую операцию над одним планом и таймаут Future. */
    public ParallelNutritionDiagnostics(Operation operation, Duration timeout) {
        this.operation = Objects.requireNonNull(operation, "Операция диагностики обязательна");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Таймаут должен быть положительным");
        }
        this.timeout = timeout;
    }

    /** Создаёт предметную диагностику со стандартным пределом ожидания. */
    public static ParallelNutritionDiagnostics standard(Duration timeout) {
        return new ParallelNutritionDiagnostics(plan -> {
            boolean attention = plan.status() == NutritionPlan.Status.NEEDS_REVIEW;
            String summary = attention
                    ? "требуется пересмотр"
                    : "состояние допустимо";
            return new PlanDiagnostic(
                    plan.id(), plan.name(), attention, summary, Thread.currentThread().isVirtual());
        }, timeout);
    }

    /**
     * Сначала фиксирует снимок, затем ожидает каждый Future и лишь после закрытия
     * executor возвращает полный пакет. Порядок итогов совпадает с порядком снимка.
     */
    public DiagnosticBatch diagnose(List<NutritionPlan> plans) {
        var snapshot = List.copyOf(Objects.requireNonNull(plans, "Снимок планов обязателен"));
        snapshot.forEach(plan -> Objects.requireNonNull(plan, "Снимок не должен содержать null"));

        var active = new AtomicInteger();
        var completed = new AtomicInteger();
        var peak = new AtomicInteger();
        var results = new ArrayList<PlanDiagnostic>(snapshot.size());
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            var futures = snapshot.stream()
                    .map(plan -> executor.submit(() -> execute(plan, active, completed, peak)))
                    .toList();
            try {
                for (var future : futures) {
                    results.add(future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
                }
            } catch (InterruptedException exception) {
                cancel(futures);
                Thread.currentThread().interrupt();
                throw new ParallelDiagnosticException(
                        "Ожидание диагностики прервано", exception);
            } catch (ExecutionException exception) {
                cancel(futures);
                throw new ParallelDiagnosticException(
                        "Диагностика плана завершилась ошибкой", exception.getCause());
            } catch (TimeoutException exception) {
                cancel(futures);
                throw new ParallelDiagnosticException(
                        "Истёк таймаут диагностики", exception);
            }
        }

        return new DiagnosticBatch(
                results, snapshot.size(), completed.get(), peak.get(), executor.isTerminated());
    }

    private PlanDiagnostic execute(
            NutritionPlan plan,
            AtomicInteger active,
            AtomicInteger completed,
            AtomicInteger peak
    ) throws Exception {
        int nowActive = active.incrementAndGet();
        peak.accumulateAndGet(nowActive, Math::max);
        try {
            var result = Objects.requireNonNull(
                    operation.diagnose(plan), "Операция не должна возвращать null");
            completed.incrementAndGet();
            return result;
        } finally {
            active.decrementAndGet();
        }
    }

    private static void cancel(List<? extends Future<?>> futures) {
        futures.forEach(future -> future.cancel(true));
    }
}
