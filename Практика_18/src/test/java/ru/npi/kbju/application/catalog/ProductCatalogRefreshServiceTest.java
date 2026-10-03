package ru.npi.kbju.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.FoodProduct;

class ProductCatalogRefreshServiceTest {
    @Test
    void refreshRunsInVirtualThreadAndPublishesOnlyImmutableCompleteSnapshot() {
        var calledInVirtualThread = new AtomicBoolean();
        var source = List.of(product("FOOD-101"), product("FOOD-102"));
        var service = new ProductCatalogRefreshService(() -> {
            calledInVirtualThread.set(Thread.currentThread().isVirtual());
            return source;
        });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = service.refreshAsync(executor).join();

            assertTrue(calledInVirtualThread.get());
            assertEquals(source, result);
            assertEquals(source, service.confirmedProducts());
            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(product("FOOD-103")));
        }
    }

    @Test
    void failureKeepsPreviousConfirmedSnapshotAndExactCause() {
        var calls = new AtomicInteger();
        var sourceFailure = new DirectoryLoadException(
                DirectoryLoadException.Kind.HTTP_STATUS, "HTTP 500");
        var service = new ProductCatalogRefreshService(() -> {
            if (calls.getAndIncrement() == 0) {
                return List.of(product("FOOD-201"));
            }
            throw sourceFailure;
        });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var confirmed = service.refreshAsync(executor).join();
            var failure = assertThrows(
                    CompletionException.class,
                    () -> service.refreshAsync(executor).join());

            assertSame(sourceFailure, failure.getCause());
            assertSame(confirmed, service.confirmedProducts());
        }
    }

    @Test
    void duplicateCodeRejectsWholeCandidateAndKeepsOldCatalog() {
        var calls = new AtomicInteger();
        var service = new ProductCatalogRefreshService(() -> {
            if (calls.getAndIncrement() == 0) {
                return List.of(product("FOOD-301"));
            }
            return List.of(product("FOOD-302"), product("FOOD-302"));
        });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var confirmed = service.refreshAsync(executor).join();

            var failure = assertThrows(
                    CompletionException.class,
                    () -> service.refreshAsync(executor).join());
            assertTrue(failure.getCause() instanceof DirectoryLoadException);
            assertEquals(DirectoryLoadException.Kind.CONTRACT,
                    ((DirectoryLoadException) failure.getCause()).kind());
            assertSame(confirmed, service.confirmedProducts());
        }
    }

    private static FoodProduct product(String code) {
        return new FoodProduct(
                code, "Продукт " + code, 100,
                new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("15"));
    }
}
