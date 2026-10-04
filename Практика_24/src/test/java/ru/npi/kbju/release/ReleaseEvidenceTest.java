package ru.npi.kbju.release;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Проверяет, что паспорт фактического выпуска входит в runtime-ресурсы. */
class ReleaseEvidenceTest {
    private static final String RESOURCE =
            "/ru/npi/kbju/release/release-manifest.txt";

    @Test
    void releaseManifestIsPackagedWithObservableAcceptanceFacts() throws Exception {
        try (var stream = getClass().getResourceAsStream(RESOURCE)) {
            assertNotNull(stream, "Манифест выпуска должен входить в classpath");
            var text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(text.contains("Выпуск: 24.0.0"));
            assertTrue(text.contains("Сценариев приёмки: 5"));
            assertTrue(text.contains("известными ограничениями"));
            assertTrue(text.contains("app-image со встроенной Java 25"));
            assertTrue(text.contains("только в новый тестовый файл"));
        }
    }
}
