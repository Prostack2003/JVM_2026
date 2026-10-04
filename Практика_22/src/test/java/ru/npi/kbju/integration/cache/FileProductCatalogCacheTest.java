package ru.npi.kbju.integration.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npi.kbju.application.catalog.CatalogCacheException;
import ru.npi.kbju.application.catalog.StoredProductCatalog;
import ru.npi.kbju.domain.FoodProduct;

class FileProductCatalogCacheTest {
    @TempDir Path directory;

    @Test
    void roundTripPreservesProductsTimestampAndLeavesNoTemporaryFile() throws Exception {
        var path = directory.resolve("catalog.json");
        var cache = new FileProductCatalogCache(path);
        var stored = new StoredProductCatalog(
                List.of(product("FOOD-101", "Кефир")),
                Instant.parse("2026-10-03T10:15:30Z"));

        cache.write(stored);
        var restored = cache.read();

        assertEquals(stored, restored);
        try (var files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
        assertThrows(UnsupportedOperationException.class,
                () -> restored.products().add(product("FOOD-102", "Творог")));
    }

    @Test
    void distinguishesMissingFileFromCorruptJsonAndContract() throws Exception {
        var path = directory.resolve("catalog.json");
        var cache = new FileProductCatalogCache(path);

        var missing = assertThrows(CatalogCacheException.class, cache::read);
        assertEquals(CatalogCacheException.Kind.NO_SAVED_DATA, missing.kind());

        Files.writeString(path, "{broken", StandardCharsets.UTF_8);
        var syntax = assertThrows(CatalogCacheException.class, cache::read);
        assertEquals(CatalogCacheException.Kind.CORRUPT_CACHE, syntax.kind());

        Files.writeString(path, """
                {"cacheVersion":1,"receivedAt":"not-an-instant","version":1,"products":[]}
                """, StandardCharsets.UTF_8);
        var timestamp = assertThrows(CatalogCacheException.class, cache::read);
        assertEquals(CatalogCacheException.Kind.CORRUPT_CACHE, timestamp.kind());
    }

    @Test
    void replacementContainsOnlyTheNewCompleteSnapshot() throws Exception {
        var path = directory.resolve("catalog.json");
        var cache = new FileProductCatalogCache(path);
        cache.write(new StoredProductCatalog(
                List.of(product("FOOD-OLD", "Старый")),
                Instant.parse("2026-10-03T10:00:00Z")));

        cache.write(new StoredProductCatalog(
                List.of(product("FOOD-NEW", "Новый")),
                Instant.parse("2026-10-03T11:00:00Z")));

        var restored = cache.read();
        assertEquals("FOOD-NEW", restored.products().getFirst().code());
        assertFalse(Files.readString(path).contains("FOOD-OLD"));
    }

    private static FoodProduct product(String code, String name) {
        return new FoodProduct(
                code, name, 100,
                new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("15"));
    }
}
