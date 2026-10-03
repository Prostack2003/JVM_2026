package ru.npi.kbju.integration.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npi.kbju.application.catalog.CatalogSnapshot;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.application.catalog.ResilientProductCatalogService;
import ru.npi.kbju.application.catalog.RetryPolicy;
import ru.npi.kbju.integration.cache.FileProductCatalogCache;

class OfflineCatalogIntegrationTest {
    @TempDir Path directory;

    @Test
    void realHttpAndFileCacheKeepLastGoodCopyAcrossServerFailureAndBadJson() throws Exception {
        var cachePath = directory.resolve("catalog.json");
        var clock = Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneOffset.UTC);

        try (var server = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var service = new ResilientProductCatalogService(
                    HttpProductDirectory.standard(server.start()),
                    new FileProductCatalogCache(cachePath),
                    clock,
                    Duration.ofHours(24),
                    new RetryPolicy(2, Duration.ZERO),
                    duration -> { });

            var fresh = service.refreshAsync(executor).join();
            assertEquals(CatalogSnapshot.Source.NETWORK, fresh.source());
            assertEquals(3, fresh.products().size());
            String confirmed = Files.readString(cachePath, StandardCharsets.UTF_8);

            int beforeFailure = server.requestCount();
            server.use(LocalProductDirectoryServer.Scenario.SERVER_ERROR);
            var cached = service.refreshAsync(executor).join();
            assertEquals(CatalogSnapshot.Source.CACHE, cached.source());
            assertEquals(2, server.requestCount() - beforeFailure);
            assertEquals(confirmed, Files.readString(cachePath, StandardCharsets.UTF_8));

            server.use(LocalProductDirectoryServer.Scenario.MALFORMED_JSON);
            var malformed = assertThrows(CompletionException.class,
                    () -> service.refreshAsync(executor).join());
            assertEquals(DirectoryLoadException.Kind.MALFORMED_JSON,
                    ((DirectoryLoadException) malformed.getCause()).kind());
            assertEquals(confirmed, Files.readString(cachePath, StandardCharsets.UTF_8));

            server.use(LocalProductDirectoryServer.Scenario.VALID);
            var restored = service.refreshAsync(executor).join();
            assertEquals(CatalogSnapshot.Source.NETWORK, restored.source());
        }
    }
}
