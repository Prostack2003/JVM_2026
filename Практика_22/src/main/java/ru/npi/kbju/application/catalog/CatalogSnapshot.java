package ru.npi.kbju.application.catalog;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ru.npi.kbju.domain.FoodProduct;

/** Результат загрузки с честным указанием источника и возраста. */
public record CatalogSnapshot(
        List<FoodProduct> products,
        Source source,
        Instant receivedAt,
        Duration age,
        int networkAttempts,
        Optional<DirectoryLoadException.Kind> fallbackReason
) {
    /** Источник показанных данных. */
    public enum Source {
        NETWORK,
        CACHE
    }

    /** Проверяет самоописание снимка. */
    public CatalogSnapshot {
        products = List.copyOf(Objects.requireNonNull(products, "Каталог обязателен"));
        source = Objects.requireNonNull(source, "Источник обязателен");
        receivedAt = Objects.requireNonNull(receivedAt, "Время получения обязательно");
        age = Objects.requireNonNull(age, "Возраст обязателен");
        fallbackReason = Objects.requireNonNull(fallbackReason, "Причина fallback обязательна");
        if (age.isNegative()) {
            throw new IllegalArgumentException("Возраст не может быть отрицательным");
        }
        if (networkAttempts < 1) {
            throw new IllegalArgumentException("Должна быть хотя бы одна сетевая попытка");
        }
        if (source == Source.NETWORK && fallbackReason.isPresent()) {
            throw new IllegalArgumentException("Свежий ответ не имеет fallback-причины");
        }
        if (source == Source.CACHE && fallbackReason.isEmpty()) {
            throw new IllegalArgumentException("Кэш должен указывать причину автономной работы");
        }
    }
}
