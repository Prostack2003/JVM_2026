package ru.npi.kbju.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Проверяет границы данных и обнаружение поставщиков отчётов. */
class NutritionReportPluginTest {
    private static final DailyNutritionSummary SUMMARY = new DailyNutritionSummary(
            LocalDate.of(2026, 10, 3), 1850, 2100, 115.0, 62.0, 210.0);

    @Test
    void serviceLoaderFindsExactlyTwoDistinctProviders() {
        var providers = ServiceLoader.load(NutritionReportPlugin.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        assertEquals(2, providers.size());
        assertEquals(2, providers.stream().map(NutritionReportPlugin::id).collect(Collectors.toSet()).size());
        assertEquals(2, providers.stream().map(provider -> provider.format(SUMMARY))
                .collect(Collectors.toSet()).size());
    }

    @Test
    void plainAndCompactProvidersKeepTheirOwnStableFormats() {
        var plain = new PlainNutritionReport().format(SUMMARY);
        var compact = new CompactNutritionReport().format(SUMMARY);

        assertTrue(plain.startsWith("Отчёт за 2026-10-03:"));
        assertTrue(plain.contains("1850/2100 ккал"));
        assertEquals("2026-10-03 | 1850/2100 ккал | Б 115.0 | Ж 62.0 | У 210.0", compact);
        assertNotEquals(plain, compact);
    }

    @Test
    void summaryRejectsInvalidCaloriesAndNutrients() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new DailyNutritionSummary(LocalDate.of(2026, 10, 3), -1, 2100, 1, 1, 1));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new DailyNutritionSummary(LocalDate.of(2026, 10, 3), 1, 0, 1, 1, 1));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> new DailyNutritionSummary(LocalDate.of(2026, 10, 3), 1, 2, Double.NaN, 1, 1));
    }
}
