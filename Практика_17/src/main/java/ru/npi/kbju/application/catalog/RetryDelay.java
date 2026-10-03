package ru.npi.kbju.application.catalog;

import java.time.Duration;

/** Граница ожидания между попытками, заменяемая в тестах. */
@FunctionalInterface
public interface RetryDelay {
    /** Приостанавливает текущую фоновую операцию. */
    void pause(Duration duration) throws InterruptedException;

    /** Возвращает реальное ожидание. */
    static RetryDelay threadSleep() {
        return duration -> Thread.sleep(duration);
    }
}
