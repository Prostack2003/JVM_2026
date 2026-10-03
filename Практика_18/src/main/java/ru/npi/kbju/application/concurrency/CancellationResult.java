package ru.npi.kbju.application.concurrency;

/** Протокол отмены ожидающей задачи и закрытия её executor. */
public record CancellationResult(
        boolean cancellationRequested,
        boolean futureCancelled,
        boolean workerInterrupted,
        boolean interruptFlagRestored,
        int activeTasksAfterClose,
        boolean executorTerminated
) { }
