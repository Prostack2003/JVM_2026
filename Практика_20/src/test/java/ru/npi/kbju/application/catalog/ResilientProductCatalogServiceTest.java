package ru.npi.kbju.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.FoodProduct;

class ResilientProductCatalogServiceTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-03T10:00:00Z");
    private static final Duration TTL = Duration.ofHours(24);

    @Test
    void successfulNetworkResultWritesConfirmedCacheAndReportsFreshSource() {
        var cache = new MemoryCache();
        var delayCalls = new AtomicInteger();
        var service = service(() -> products(), cache, RECEIVED_AT,
                duration -> delayCalls.incrementAndGet());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = service.refreshAsync(executor).join();

            assertEquals(CatalogSnapshot.Source.NETWORK, result.source());
            assertEquals(Duration.ZERO, result.age());
            assertEquals(1, result.networkAttempts());
            assertTrue(result.fallbackReason().isEmpty());
            assertEquals(new StoredProductCatalog(products(), RECEIVED_AT), cache.stored);
            assertEquals(0, delayCalls.get());
        }
    }

    @Test
    void retriesTemporary503OnceThenUsesFreshCacheWithReasonAndAge() {
        var calls = new AtomicInteger();
        var delays = new AtomicInteger();
        var cache = new MemoryCache();
        cache.stored = new StoredProductCatalog(products(), RECEIVED_AT.minusSeconds(600));
        var service = service(() -> {
            calls.incrementAndGet();
            throw DirectoryLoadException.httpStatus(503);
        }, cache, RECEIVED_AT, duration -> delays.incrementAndGet());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = service.refreshAsync(executor).join();

            assertEquals(CatalogSnapshot.Source.CACHE, result.source());
            assertEquals(Duration.ofMinutes(10), result.age());
            assertEquals(2, result.networkAttempts());
            assertEquals(DirectoryLoadException.Kind.HTTP_STATUS,
                    result.fallbackReason().orElseThrow());
            assertEquals(2, calls.get());
            assertEquals(1, delays.get());
        }
    }

    @Test
    void contractFailureDoesNotRetryReadCacheOrOverwriteIt() {
        var calls = new AtomicInteger();
        var cache = new MemoryCache();
        var old = new StoredProductCatalog(products(), RECEIVED_AT.minusSeconds(60));
        cache.stored = old;
        var original = new DirectoryLoadException(
                DirectoryLoadException.Kind.MALFORMED_JSON, "broken");
        var service = service(() -> {
            calls.incrementAndGet();
            throw original;
        }, cache, RECEIVED_AT, duration -> { });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var thrown = assertThrows(CompletionException.class,
                    () -> service.refreshAsync(executor).join());

            assertSame(original, thrown.getCause());
            assertEquals(1, calls.get());
            assertEquals(0, cache.reads);
            assertEquals(0, cache.writes);
            assertSame(old, cache.stored);
        }
    }

    @Test
    void authorizationFailureDoesNotRetryOrHideBehindCache() {
        var calls = new AtomicInteger();
        var cache = new MemoryCache();
        cache.stored = new StoredProductCatalog(products(), RECEIVED_AT);
        var service = service(() -> {
            calls.incrementAndGet();
            throw DirectoryLoadException.httpStatus(401);
        }, cache, RECEIVED_AT, duration -> { });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var thrown = assertThrows(CompletionException.class,
                    () -> service.refreshAsync(executor).join());

            assertEquals(DirectoryLoadException.Kind.HTTP_STATUS,
                    ((DirectoryLoadException) thrown.getCause()).kind());
            assertEquals(1, calls.get());
            assertEquals(0, cache.reads);
        }
    }

    @Test
    void missingAndCorruptCacheHaveDifferentUserVisibleStates() {
        assertCacheFailure(
                CatalogCacheException.Kind.NO_SAVED_DATA,
                CatalogUnavailableException.Kind.NO_SAVED_DATA);
        assertCacheFailure(
                CatalogCacheException.Kind.CORRUPT_CACHE,
                CatalogUnavailableException.Kind.CORRUPT_CACHE);
    }

    @Test
    void cacheIsAcceptedAtExactTtlButRejectedOneNanosecondLaterAndAfterClockRollback() {
        var cache = new MemoryCache();
        cache.stored = new StoredProductCatalog(products(), RECEIVED_AT);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var boundary = service(temporaryFailure(), cache, RECEIVED_AT.plus(TTL), duration -> { })
                    .refreshAsync(executor).join();
            assertEquals(TTL, boundary.age());

            assertUnavailable(
                    service(temporaryFailure(), cache, RECEIVED_AT.plus(TTL).plusNanos(1),
                            duration -> { }),
                    executor,
                    CatalogUnavailableException.Kind.EXPIRED_CACHE);
            assertUnavailable(
                    service(temporaryFailure(), cache, RECEIVED_AT.minusNanos(1), duration -> { }),
                    executor,
                    CatalogUnavailableException.Kind.CLOCK_ROLLBACK);
        }
    }

    @Test
    void secondParallelRefreshIsRejectedButManualRetryWorksAfterCompletion() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var service = service(() -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("release timeout");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new DirectoryLoadException(
                        DirectoryLoadException.Kind.INTERRUPTED, "interrupted", failure);
            }
            return products();
        }, new MemoryCache(), RECEIVED_AT, duration -> { });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = service.refreshAsync(executor);
            assertTrue(entered.await(2, TimeUnit.SECONDS));

            var concurrent = assertThrows(CompletionException.class,
                    () -> service.refreshAsync(executor).join());
            assertEquals(CatalogUnavailableException.Kind.UPDATE_IN_PROGRESS,
                    ((CatalogUnavailableException) concurrent.getCause()).kind());

            release.countDown();
            assertEquals(CatalogSnapshot.Source.NETWORK, first.join().source());
            assertEquals(CatalogSnapshot.Source.NETWORK,
                    service.refreshAsync(executor).join().source());
            assertEquals(2, calls.get());
        }
    }

    private static void assertCacheFailure(
            CatalogCacheException.Kind cacheKind,
            CatalogUnavailableException.Kind expected
    ) {
        var cache = new MemoryCache();
        cache.readFailure = new CatalogCacheException(cacheKind, "cache failure");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertUnavailable(
                    service(temporaryFailure(), cache, RECEIVED_AT, duration -> { }),
                    executor,
                    expected);
        }
    }

    private static void assertUnavailable(
            ResilientProductCatalogService service,
            java.util.concurrent.Executor executor,
            CatalogUnavailableException.Kind expected
    ) {
        var failure = assertThrows(CompletionException.class,
                () -> service.refreshAsync(executor).join());
        assertEquals(expected, ((CatalogUnavailableException) failure.getCause()).kind());
    }

    private static ProductDirectory temporaryFailure() {
        return () -> {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.TRANSPORT, "offline");
        };
    }

    private static ResilientProductCatalogService service(
            ProductDirectory remote,
            ProductCatalogCache cache,
            Instant now,
            RetryDelay delay
    ) {
        return new ResilientProductCatalogService(
                remote,
                cache,
                Clock.fixed(now, ZoneOffset.UTC),
                TTL,
                new RetryPolicy(2, Duration.ofMillis(10)),
                delay);
    }

    private static List<FoodProduct> products() {
        return List.of(new FoodProduct(
                "FOOD-101", "Кефир", 53,
                new BigDecimal("3.0"), new BigDecimal("2.5"), new BigDecimal("4.0")));
    }

    private static final class MemoryCache implements ProductCatalogCache {
        private StoredProductCatalog stored;
        private CatalogCacheException readFailure;
        private int reads;
        private int writes;

        @Override
        public StoredProductCatalog read() {
            reads++;
            if (readFailure != null) {
                throw readFailure;
            }
            return stored;
        }

        @Override
        public void write(StoredProductCatalog catalog) {
            writes++;
            stored = catalog;
        }
    }
}
