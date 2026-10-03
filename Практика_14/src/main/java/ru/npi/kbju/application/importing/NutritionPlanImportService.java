package ru.npi.kbju.application.importing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import ru.npi.kbju.domain.BatchImportException;
import ru.npi.kbju.domain.NutritionPlanBatchRepository;

/** Координирует полную проверку CSV и одну атомарную операцию репозитория. */
public final class NutritionPlanImportService {
    private final NutritionPlanBatchRepository repository;

    /** Получает порт пакетного хранения через конструктор. */
    public NutritionPlanImportService(NutritionPlanBatchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Репозиторий импорта обязателен");
    }

    /** Проверяет весь файл, затем передаёт готовые строки в одну транзакцию. */
    public ImportSummary importCsv(Path path)
            throws IOException, CsvImportException, BatchImportException {
        var rows = NutritionPlanCsvParser.parse(path);
        return new ImportSummary(repository.importBatch(rows));
    }
}
