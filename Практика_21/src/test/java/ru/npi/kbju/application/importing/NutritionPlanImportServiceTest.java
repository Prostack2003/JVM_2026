package ru.npi.kbju.application.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npi.kbju.domain.ImportedNutritionPlan;
import ru.npi.kbju.domain.NutritionPlanBatchRepository;
import ru.npi.kbju.domain.NutritionPlanImportRow;

/** Проверяет, что транзакционный порт вызывается только после проверки всего файла. */
class NutritionPlanImportServiceTest {
    @TempDir
    Path directory;

    @Test
    void invalidLaterRowPreventsAnyRepositoryCall() throws Exception {
        var csv = directory.resolve("invalid-later-row.csv");
        Files.writeString(csv, """
                code;name;effective_from
                KBJU-801;Первая корректная строка;2026-11-01
                KBJU-802;Некорректная строка;не-дата
                """, StandardCharsets.UTF_8);
        var called = new AtomicBoolean();
        var service = new NutritionPlanImportService(new RecordingRepository(called));

        var failure = assertThrowsExactly(CsvImportException.class,
                () -> service.importCsv(csv));

        assertEquals(3, failure.lineNumber());
        assertFalse(called.get());
    }

    private record RecordingRepository(AtomicBoolean called)
            implements NutritionPlanBatchRepository {
        @Override
        public List<ImportedNutritionPlan> importBatch(List<NutritionPlanImportRow> rows) {
            called.set(true);
            return List.of();
        }

        @Override
        public Optional<ImportedNutritionPlan> findImportedByCode(String code) {
            return Optional.empty();
        }
    }
}
