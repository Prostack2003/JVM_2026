package ru.npi.kbju.lesson;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ru.npi.kbju.application.catalog.CatalogSnapshot;
import ru.npi.kbju.application.catalog.CatalogUnavailableException;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.application.catalog.ResilientProductCatalogService;
import ru.npi.kbju.application.catalog.RetryDelay;
import ru.npi.kbju.application.catalog.RetryPolicy;
import ru.npi.kbju.integration.cache.FileProductCatalogCache;
import ru.npi.kbju.integration.http.HttpProductDirectory;
import ru.npi.kbju.integration.http.LocalProductDirectoryServer;

/** Воспроизводимая приёмка кэша, retry, TTL и автономных отказов. */
public final class OfflineCatalogDemo {
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-03T10:00:00Z");
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private OfflineCatalogDemo() { }

    /** Показывает fresh, cache, no-cache, corrupt-cache и границу TTL. */
    public static void main(String[] args) throws IOException {
        System.out.println("Практика 16: кэш, повторный запрос и автономный режим");
        Path directory = Files.createTempDirectory("kbju-practice-16-");
        Path cachePath = directory.resolve("products-cache.json");
        Path emptyCachePath = directory.resolve("empty-cache.json");
        Path corruptCachePath = directory.resolve("corrupt-cache.json");
        var worker = Executors.newVirtualThreadPerTaskExecutor();
        int port;
        URI endpoint;

        try (worker; var server = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID)) {
            endpoint = server.start();
            port = server.port();
            var service = service(endpoint, cachePath, fixed(RECEIVED_AT));

            CatalogSnapshot fresh = await(service.refreshAsync(worker));
            require(fresh.source() == CatalogSnapshot.Source.NETWORK, "Ожидалась сеть");
            require(Files.isRegularFile(cachePath), "Кэш должен быть создан");
            System.out.println("Свежий ответ: источник=сеть, получено="
                    + fresh.receivedAt() + ", записей=" + fresh.products().size());

            int beforeFailure = server.requestCount();
            server.use(LocalProductDirectoryServer.Scenario.SERVER_ERROR);
            CatalogSnapshot cached = await(service.refreshAsync(worker));
            int attempts = server.requestCount() - beforeFailure;
            require(cached.source() == CatalogSnapshot.Source.CACHE, "Ожидался кэш");
            require(attempts == 2, "Должно быть ровно две попытки");
            System.out.println("Автономно: источник=кэш, возраст="
                    + cached.age().toMinutes() + " мин, причина="
                    + cached.fallbackReason().orElseThrow() + ", попыток=" + attempts);

            String confirmedCache = Files.readString(cachePath, StandardCharsets.UTF_8);
            server.use(LocalProductDirectoryServer.Scenario.MALFORMED_JSON);
            expectDirectoryFailure(
                    service.refreshAsync(worker), DirectoryLoadException.Kind.MALFORMED_JSON);
            require(confirmedCache.equals(Files.readString(cachePath, StandardCharsets.UTF_8)),
                    "Повреждённый ответ не должен менять кэш");
            System.out.println("Повреждённый ответ: MALFORMED_JSON, подтверждённый кэш не изменён");
        }

        try (var offlineWorker = Executors.newVirtualThreadPerTaskExecutor()) {
            var noCache = service(endpoint, emptyCachePath, fixed(RECEIVED_AT.plusSeconds(60)));
            expectUnavailable(
                    noCache.refreshAsync(offlineWorker),
                    CatalogUnavailableException.Kind.NO_SAVED_DATA);
            System.out.println("Нет сохранённых данных; действие=повторить запрос");

            Files.writeString(corruptCachePath, "{broken", StandardCharsets.UTF_8);
            var corrupt = service(endpoint, corruptCachePath, fixed(RECEIVED_AT.plusSeconds(60)));
            expectUnavailable(
                    corrupt.refreshAsync(offlineWorker),
                    CatalogUnavailableException.Kind.CORRUPT_CACHE);
            System.out.println("Повреждённый кэш: CORRUPT_CACHE; неподтверждённые данные не показаны");

            CatalogSnapshot boundary = await(service(
                    endpoint, cachePath, fixed(RECEIVED_AT.plus(CACHE_TTL)))
                    .refreshAsync(offlineWorker));
            require(boundary.source() == CatalogSnapshot.Source.CACHE,
                    "Ровно на границе TTL кэш ещё допустим");
            expectUnavailable(
                    service(endpoint, cachePath, fixed(RECEIVED_AT.plus(CACHE_TTL).plusSeconds(1)))
                            .refreshAsync(offlineWorker),
                    CatalogUnavailableException.Kind.EXPIRED_CACHE);
            System.out.println("Граница TTL: 24 ч допустимы; 24 ч 1 с — EXPIRED_CACHE");
        }

        try (var restored = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID, port);
             var retryWorker = Executors.newVirtualThreadPerTaskExecutor()) {
            URI restoredEndpoint = restored.start();
            CatalogSnapshot retried = await(
                    service(restoredEndpoint, emptyCachePath, fixed(RECEIVED_AT.plusSeconds(120)))
                            .refreshAsync(retryWorker));
            require(retried.source() == CatalogSnapshot.Source.NETWORK,
                    "Ручной повтор должен получить свежие данные");
            System.out.println("Повтор после восстановления: источник=сеть, порт=" + port);
        } finally {
            Files.deleteIfExists(cachePath);
            Files.deleteIfExists(emptyCachePath);
            Files.deleteIfExists(corruptCachePath);
            Files.deleteIfExists(directory);
        }
        System.out.println("Итог: все сценарии практики 16 подтверждены.");
    }

    private static ResilientProductCatalogService service(
            URI endpoint,
            Path cache,
            Clock clock
    ) {
        return new ResilientProductCatalogService(
                HttpProductDirectory.standard(endpoint),
                new FileProductCatalogCache(cache),
                clock,
                CACHE_TTL,
                RetryPolicy.standard(),
                RetryDelay.threadSleep());
    }

    private static Clock fixed(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static CatalogSnapshot await(CompletableFuture<CatalogSnapshot> future) {
        try {
            return future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание прервано", failure);
        } catch (ExecutionException failure) {
            throw new IllegalStateException("Ожидался успешный сценарий", failure.getCause());
        } catch (TimeoutException failure) {
            future.cancel(true);
            throw new IllegalStateException("Истёк таймаут демонстрации", failure);
        }
    }

    private static void expectDirectoryFailure(
            CompletableFuture<CatalogSnapshot> future,
            DirectoryLoadException.Kind expected
    ) {
        Throwable failure = awaitFailure(future);
        require(failure instanceof DirectoryLoadException, "Ожидался сетевой отказ");
        require(((DirectoryLoadException) failure).kind() == expected,
                "Ожидался " + expected);
    }

    private static void expectUnavailable(
            CompletableFuture<CatalogSnapshot> future,
            CatalogUnavailableException.Kind expected
    ) {
        Throwable failure = awaitFailure(future);
        require(failure instanceof CatalogUnavailableException,
                "Ожидался прикладной отказ автономной работы");
        require(((CatalogUnavailableException) failure).kind() == expected,
                "Ожидался " + expected);
    }

    private static Throwable awaitFailure(CompletableFuture<CatalogSnapshot> future) {
        try {
            future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            throw new AssertionError("Ожидался отказ");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание прервано", failure);
        } catch (ExecutionException failure) {
            return failure.getCause();
        } catch (TimeoutException failure) {
            future.cancel(true);
            throw new IllegalStateException("Истёк таймаут демонстрации", failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
