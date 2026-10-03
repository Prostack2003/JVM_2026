package ru.npi.kbju.application.concurrency;

import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Воспроизводит потерю обновления и сразу сравнивает её с атомарным вариантом. */
public final class DeterministicCounterRace {
    private static final int TASK_COUNT = 2;
    private final Duration timeout;

    /** Задаёт предел ожидания барьера и результатов. */
    public DeterministicCounterRace(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Таймаут должен быть положительным");
        }
        this.timeout = timeout;
    }

    /**
     * Заставляет обе задачи прочитать одно и то же значение до записи.
     * Поэтому обычный read-modify-write стабильно даёт 1, а AtomicInteger — 2.
     */
    public CounterRaceResult run() {
        var unsafe = new int[] {0};
        var atomic = new AtomicInteger();
        var barrier = new CyclicBarrier(TASK_COUNT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            Callable<Void> increment = () -> {
                incrementBoth(unsafe, atomic, barrier);
                return null;
            };
            var first = executor.submit(increment);
            var second = executor.submit(increment);
            await(first);
            await(second);
        }

        return new CounterRaceResult(
                TASK_COUNT, unsafe[0], atomic.get(), executor.isTerminated());
    }

    private void incrementBoth(
            int[] unsafe,
            AtomicInteger atomic,
            CyclicBarrier barrier
    ) throws InterruptedException, BrokenBarrierException, TimeoutException {
        int observed = unsafe[0];
        barrier.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        unsafe[0] = observed + 1;
        atomic.incrementAndGet();
    }

    private void await(Future<?> future) {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParallelDiagnosticException(
                    "Ожидание учебной гонки прервано", exception);
        } catch (ExecutionException exception) {
            throw new ParallelDiagnosticException(
                    "Задача учебной гонки завершилась ошибкой", exception.getCause());
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new ParallelDiagnosticException(
                    "Истёк таймаут учебной гонки", exception);
        }
    }
}
