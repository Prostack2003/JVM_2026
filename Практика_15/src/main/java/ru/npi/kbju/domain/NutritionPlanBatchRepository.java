package ru.npi.kbju.domain;

import java.util.List;
import java.util.Optional;

/** Порт атомарной пакетной записи, отдельный от обычных CRUD-операций. */
public interface NutritionPlanBatchRepository {
    /** Сохраняет весь пакет одной транзакцией либо не сохраняет ничего. */
    List<ImportedNutritionPlan> importBatch(List<NutritionPlanImportRow> rows)
            throws BatchImportException;

    /** Ищет ранее импортированный план по нормализованному коду CSV. */
    Optional<ImportedNutritionPlan> findImportedByCode(String code);
}
