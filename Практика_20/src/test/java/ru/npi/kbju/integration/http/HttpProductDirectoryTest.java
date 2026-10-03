package ru.npi.kbju.integration.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.application.catalog.DirectoryLoadException;

class HttpProductDirectoryTest {
    @Test
    void http200ReturnsFullyMappedCatalog() {
        try (var server = server(LocalProductDirectoryServer.Scenario.VALID)) {
            var products = HttpProductDirectory.standard(server.start()).fetch();

            assertEquals(3, products.size());
            assertEquals("FOOD-001", products.getFirst().code());
        }
    }

    @Test
    void http500IsRejectedBeforeErrorBodyCanBeParsedAsData() {
        try (var server = server(LocalProductDirectoryServer.Scenario.SERVER_ERROR)) {
            var client = HttpProductDirectory.standard(server.start());

            var failure = assertThrows(DirectoryLoadException.class, client::fetch);
            assertEquals(DirectoryLoadException.Kind.HTTP_STATUS, failure.kind());
            assertEquals(500, failure.httpStatusCode().orElseThrow());
            assertTrue(failure.getMessage().contains("HTTP 500"));
        }
    }

    @Test
    void successfulStatusWithBrokenJsonHasSeparateFailureKind() {
        try (var server = server(LocalProductDirectoryServer.Scenario.MALFORMED_JSON)) {
            var failure = assertThrows(
                    DirectoryLoadException.class,
                    HttpProductDirectory.standard(server.start())::fetch);

            assertEquals(DirectoryLoadException.Kind.MALFORMED_JSON, failure.kind());
        }
    }

    @Test
    void validJsonWithImpossibleNutritionValueFailsContract() {
        try (var server = server(LocalProductDirectoryServer.Scenario.INVALID_PRODUCT)) {
            var failure = assertThrows(
                    DirectoryLoadException.class,
                    HttpProductDirectory.standard(server.start())::fetch);

            assertEquals(DirectoryLoadException.Kind.CONTRACT, failure.kind());
            assertTrue(failure.getMessage().contains("energy")
                    || failure.getMessage().contains("Энерг"));
        }
    }

    @Test
    void requestTimeoutIsReportedSeparately() {
        try (var server = server(LocalProductDirectoryServer.Scenario.DELAYED)) {
            var client = new HttpProductDirectory(
                    server.start(), Duration.ofSeconds(1), Duration.ofMillis(50),
                    HttpProductDirectory.DEFAULT_MAX_RESPONSE_BYTES);

            var failure = assertThrows(DirectoryLoadException.class, client::fetch);
            assertEquals(DirectoryLoadException.Kind.TIMEOUT, failure.kind());
        }
    }

    @Test
    void closingServerReleasesExactPortForNextRun() {
        int port;
        try (var first = server(LocalProductDirectoryServer.Scenario.VALID)) {
            first.start();
            port = first.port();
        }

        try (var second = new LocalProductDirectoryServer(
                LocalProductDirectoryServer.Scenario.VALID, port)) {
            assertEquals(3, HttpProductDirectory.standard(second.start()).fetch().size());
            assertEquals(port, second.port());
        }
    }

    private static LocalProductDirectoryServer server(
            LocalProductDirectoryServer.Scenario scenario
    ) {
        return new LocalProductDirectoryServer(scenario);
    }
}
