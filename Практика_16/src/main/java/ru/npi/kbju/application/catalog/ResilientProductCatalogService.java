package ru.npi.kbju.application.catalog;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.npi.kbju.domain.FoodProduct;

/** Загружает справочник с bounded retry и честным cache fallback. */
public final class ResilientProductCatalogService {
    private final ProductDirectory remote;
    private final ProductCatalogCache cache;
    private final Clock clock;
    private final Duration maxCacheAge;
    private final RetryPolicy retryPolicy;
    private final RetryDelay retryDelay;
    private final AtomicBoolean updateInProgress = new AtomicBoolean();

    /** Создаёт сервис с явными часами, TTL и политикой повтора. */
    public ResilientProductCatalogService(
            ProductDirectory remote,
            ProductCatalogCache cache,
            Clock clock,
            Duration maxCacheAge,
            RetryPolicy retryPolicy,
            RetryDelay retryDelay
    ) {
        this.remote = Objects.requireNonNull(remote, "Внешний справочник обязателен");
        this.cache = Objects.requireNonNull(cache, "Кэш обязателен");
        this.clock = Objects.requireNonNull(clock, "Часы обязательны");
        this.maxCacheAge = requirePositive(maxCacheAge, "TTL кэша");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "Политика retry обязательна");
        this.retryDelay = Objects.requireNonNull(retryDelay, "Ожидание retry обязательно");
    }

    /** Запускает одно фоновое обновление; второе параллельное отклоняется. */
    public CompletableFuture<CatalogSnapshot> refreshAsync(Executor executor) {
        Objects.requireNonNull(executor, "Executor обязателен");
        if (!updateInProgress.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.UPDATE_IN_PROGRESS,
                    "Обновление справочника уже выполняется",
                    null));
        }
        try {
            return CompletableFuture.supplyAsync(this::refresh, executor)
                    .whenComplete((result, failure) -> updateInProgress.set(false));
        } catch (RuntimeException failure) {
            updateInProgress.set(false);
            throw failure;
        }
    }

    private CatalogSnapshot refresh() {
        DirectoryLoadException lastFailure = null;
        int attempt = 0;
        while (attempt < retryPolicy.maxAttempts()) {
            attempt++;
            try {
                List<FoodProduct> products = validate(remote.fetch());
                var receivedAt = clock.instant();
                cache.write(new StoredProductCatalog(products, receivedAt));
                return new CatalogSnapshot(
                        products,
                        CatalogSnapshot.Source.NETWORK,
                        receivedAt,
                        Duration.ZERO,
                        attempt,
                        Optional.empty());
            } catch (DirectoryLoadException failure) {
                lastFailure = failure;
                if (!allowsCacheFallback(failure)) {
                    throw failure;
                }
                if (attempt < retryPolicy.maxAttempts()) {
                    pauseBeforeRetry();
                }
            }
        }
        return fromCache(Objects.requireNonNull(lastFailure), attempt);
    }

    private CatalogSnapshot fromCache(DirectoryLoadException networkFailure, int attempts) {
        final StoredProductCatalog stored;
        try {
            stored = Objects.requireNonNull(cache.read(), "Кэш не должен возвращать null");
        } catch (CatalogCacheException cacheFailure) {
            networkFailure.addSuppressed(cacheFailure);
            throw unavailableFor(cacheFailure.kind(), networkFailure);
        }

        var now = clock.instant();
        var age = Duration.between(stored.receivedAt(), now);
        if (age.isNegative()) {
            throw new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.CLOCK_ROLLBACK,
                    "Время кэша находится в будущем; проверьте системные часы",
                    networkFailure);
        }
        if (age.compareTo(maxCacheAge) > 0) {
            throw new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.EXPIRED_CACHE,
                    "Сохранённый справочник просрочен: возраст " + age.toMinutes() + " минут",
                    networkFailure);
        }
        var products = validate(stored.products());
        return new CatalogSnapshot(
                products,
                CatalogSnapshot.Source.CACHE,
                stored.receivedAt(),
                age,
                attempts,
                Optional.of(networkFailure.kind()));
    }

    private void pauseBeforeRetry() {
        try {
            retryDelay.pause(retryPolicy.delay());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.INTERRUPTED,
                    "Ожидание перед повтором прервано",
                    failure);
        }
    }

    private static boolean allowsCacheFallback(DirectoryLoadException failure) {
        return switch (failure.kind()) {
            case TIMEOUT, TRANSPORT -> true;
            case HTTP_STATUS -> failure.httpStatusCode().stream()
                    .anyMatch(status -> status == 429 || status >= 500);
            case MALFORMED_JSON, CONTRACT, INTERRUPTED -> false;
        };
    }

    private static CatalogUnavailableException unavailableFor(
            CatalogCacheException.Kind cacheKind,
            DirectoryLoadException networkFailure
    ) {
        return switch (cacheKind) {
            case NO_SAVED_DATA -> new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.NO_SAVED_DATA,
                    "Нет сохранённых данных; доступно действие «Повторить запрос»",
                    networkFailure);
            case CORRUPT_CACHE -> new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.CORRUPT_CACHE,
                    "Сохранённый каталог повреждён; нужен повтор после восстановления связи",
                    networkFailure);
            case READ_FAILED, WRITE_FAILED -> new CatalogUnavailableException(
                    CatalogUnavailableException.Kind.CACHE_READ_FAILED,
                    "Не удалось прочитать сохранённый каталог",
                    networkFailure);
        };
    }

    private static List<FoodProduct> validate(List<FoodProduct> products) {
        var snapshot = List.copyOf(Objects.requireNonNull(products, "Каталог обязателен"));
        var codes = new HashSet<String>();
        for (var product : snapshot) {
            Objects.requireNonNull(product, "Каталог не должен содержать null");
            if (!codes.add(product.code())) {
                throw new DirectoryLoadException(
                        DirectoryLoadException.Kind.CONTRACT,
                        "В каталоге повторяется код " + product.code());
            }
        }
        return snapshot;
    }

    private static Duration requirePositive(Duration value, String field) {
        Objects.requireNonNull(value, field + " обязателен");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " должен быть положительным");
        }
        return value;
    }
}
