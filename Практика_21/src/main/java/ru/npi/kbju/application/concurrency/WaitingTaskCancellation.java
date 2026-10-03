package ru.npi.kbju.application.concurrency;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Воспроизводит кооперативную отмену задачи, заблокированной в ожидании. */
public final class WaitingTaskCancellation {
    private final Duration timeout;

    /** Задаёт предел ожидания запуска и отмены. */
    public WaitingTaskCancellation(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Таймаут должен быть положительным");
        }
        this.timeout = timeout;
    }

    /** Дожидается фактического запуска, отменяет Future с прерыванием и закрывает executor. */
    public CancellationResult run() {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var active = new AtomicInteger();
        var interrupted = new AtomicBoolean();
        var interruptRestored = new AtomicBoolean();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        boolean cancellationRequested;
        boolean futureCancelled;

        try (executor) {
            var future = executor.submit(() -> {
                active.incrementAndGet();
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    interrupted.set(true);
                    Thread.currentThread().interrupt();
                    interruptRestored.set(Thread.currentThread().isInterrupted());
                    throw exception;
                } finally {
                    active.decrementAndGet();
                }
                return null;
            });

            try {
                awaitStart(started);
                cancellationRequested = future.cancel(true);
                futureCancelled = future.isCancelled();
                try {
                    future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    throw new IllegalStateException(
                            "Отменённая задача не должна возвращать результат");
                } catch (java.util.concurrent.CancellationException expected) {
                    // Future явно сообщает об отмене; это ожидаемый исход.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new ParallelDiagnosticException(
                            "Ожидание отмены прервано", exception);
                } catch (ExecutionException | TimeoutException exception) {
                    throw new ParallelDiagnosticException(
                            "Не удалось подтвердить отмену", exception);
                }
            } finally {
                // Даже при таймауте снимаем ожидание до executor.close(), чтобы close не завис.
                release.countDown();
            }
        }

        return new CancellationResult(
                cancellationRequested,
                futureCancelled,
                interrupted.get(),
                interruptRestored.get(),
                active.get(),
                executor.isTerminated());
    }

    private void awaitStart(CountDownLatch started) {
        try {
            if (!started.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new ParallelDiagnosticException(
                        "Ожидающая задача не запустилась вовремя", new TimeoutException());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParallelDiagnosticException(
                    "Ожидание запуска задачи прервано", exception);
        }
    }
}
