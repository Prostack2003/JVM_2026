package ru.npi.kbju.application.importing;

import java.util.List;
import ru.npi.kbju.domain.ImportedNutritionPlan;

/** Неизменяемый результат успешно подтверждённого пакета. */
public record ImportSummary(List<ImportedNutritionPlan> imported) {
    /** Создаёт независимый неизменяемый снимок. */
    public ImportSummary {
        imported = List.copyOf(imported);
    }

    /** Возвращает число строк, подтверждённых одной транзакцией. */
    public int importedCount() {
        return imported.size();
    }
}
