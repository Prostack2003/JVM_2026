package ru.npi.kbju.application.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Проверяет полный разбор и диагностику учебного CSV до обращения к БД. */
class NutritionPlanCsvParserTest {
    @TempDir
    Path directory;

    @Test
    void validFileIsFullyParsedAndNormalized() throws Exception {
        var csv = write("valid.csv", """
                code;name;effective_from
                  kbju-301 ; План на неделю ;2026-11-03
                KBJU_302;Поддержание массы;2026-11-04
                """);

        var rows = NutritionPlanCsvParser.parse(csv);

        assertEquals(2, rows.size());
        assertEquals("KBJU-301", rows.get(0).code());
        assertEquals("План на неделю", rows.get(0).name());
        assertEquals(LocalDate.of(2026, 11, 3), rows.get(0).effectiveFrom());
        assertEquals("KBJU_302", rows.get(1).code());
    }

    @Test
    void malformedDateReportsExactLine() throws Exception {
        var csv = write("bad-date.csv", """
                code;name;effective_from
                KBJU-401;План;2026-99-01
                """);

        var failure = assertThrowsExactly(
                CsvImportException.class, () -> NutritionPlanCsvParser.parse(csv));

        assertEquals(2, failure.lineNumber());
        assertTrue(failure.getMessage().contains("Строка 2"));
    }

    @Test
    void unsupportedQuotesAreRejectedExplicitly() throws Exception {
        var csv = write("quoted.csv", """
                code;name;effective_from
                KBJU-402;"План;на неделю";2026-11-03
                """);

        var failure = assertThrowsExactly(
                CsvImportException.class, () -> NutritionPlanCsvParser.parse(csv));

        assertEquals(2, failure.lineNumber());
        assertTrue(failure.getMessage().contains("кавычки"));
    }

    @Test
    void wrongHeaderIsRejectedBeforeAnyRows() throws Exception {
        var csv = write("wrong-header.csv", """
                name;code;effective_from
                План;KBJU-403;2026-11-03
                """);

        var failure = assertThrowsExactly(
                CsvImportException.class, () -> NutritionPlanCsvParser.parse(csv));

        assertEquals(1, failure.lineNumber());
        assertTrue(failure.getMessage().contains("code;name;effective_from"));
    }

    private Path write(String name, String content) throws Exception {
        var target = directory.resolve(name);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }
}
