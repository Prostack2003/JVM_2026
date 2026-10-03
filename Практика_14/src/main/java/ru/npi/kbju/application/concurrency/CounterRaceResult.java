package ru.npi.kbju.application.concurrency;

/** Результат детерминированной гонки двух инкрементов. */
public record CounterRaceResult(
        int expectedCount,
        int unsafeCount,
        int atomicCount,
        boolean executorTerminated
) {
    /** Защищает контракт учебного сценария. */
    public CounterRaceResult {
        if (expectedCount < 0 || unsafeCount < 0 || atomicCount < 0) {
            throw new IllegalArgumentException("Счётчики не могут быть отрицательными");
        }
    }
}
