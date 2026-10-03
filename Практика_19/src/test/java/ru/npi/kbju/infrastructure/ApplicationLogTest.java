package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Проверяет полезность диагностических событий и запрет чувствительных данных. */
class ApplicationLogTest {
    private static final String SECRET = "TEST-TOKEN-8f31c9";
    private static final String PRIVATE_PLAN = "Диета после медицинского обследования";

    @TempDir
    Path directory;

    @Test
    void failureContainsOperationReasonAndEventIdButNotSensitiveCause() throws Exception {
        var config = AppConfig.load(directory.resolve("data"),
                directory.resolve("missing.properties"));
        final String eventId;

        try (var log = ApplicationLog.open(directory.resolve("logs"))) {
            log.recordApplicationStarted(config);
            eventId = log.recordFailure(
                    ApplicationLog.Operation.PLAN_SAVE,
                    ApplicationLog.FailureReason.STORAGE_UNAVAILABLE,
                    new IOException("Authorization=" + SECRET + "; plan=" + PRIVATE_PLAN)
            );
        }

        var content = readLogs(directory.resolve("logs"));
        assertTrue(content.contains("event=application_started"));
        assertTrue(content.contains("event=operation_failed"));
        assertTrue(content.contains("eventId=" + eventId));
        assertTrue(content.contains("operation=PLAN_SAVE"));
        assertTrue(content.contains("reason=STORAGE_UNAVAILABLE"));
        assertFalse(content.contains(SECRET));
        assertFalse(content.contains(PRIVATE_PLAN));
        assertFalse(content.contains("Authorization"));
    }

    @Test
    void closingLogReleasesHandlerAndAllowsNextSession() throws Exception {
        var logs = directory.resolve("reopen-logs");
        var config = AppConfig.load(directory.resolve("data"),
                directory.resolve("missing.properties"));

        try (var first = ApplicationLog.open(logs)) {
            first.recordApplicationStarted(config);
        }
        try (var second = ApplicationLog.open(logs)) {
            second.recordApplicationStarted(config);
        }

        assertTrue(readLogs(logs).lines()
                .filter(line -> line.contains("event=application_started"))
                .count() >= 2);
    }

    private static String readLogs(Path directory) throws Exception {
        var result = new StringBuilder();
        try (var paths = Files.list(directory)) {
            for (var path : paths
                    .filter(file -> file.getFileName().toString().endsWith(".log"))
                    .sorted()
                    .toList()) {
                result.append(Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        return result.toString();
    }
}
