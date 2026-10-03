package ru.npi.kbju.application.catalog;

import java.time.Duration;
import java.util.Objects;

/** Ограниченная политика повтора GET-запроса. */
public record RetryPolicy(int maxAttempts, Duration delay) {
    /** Отклоняет бесконечные и отрицательные настройки. */
    public RetryPolicy {
        delay = Objects.requireNonNull(delay, "Задержка обязательна");
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("Число попыток должно быть от 1 до 10");
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("Задержка не может быть отрицательной");
        }
    }

    /** Политика для учебного GET: первая попытка и один повтор. */
    public static RetryPolicy standard() {
        return new RetryPolicy(2, Duration.ofMillis(100));
    }
}
