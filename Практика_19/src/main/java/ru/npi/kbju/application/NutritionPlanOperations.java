package ru.npi.kbju.application;

import java.time.LocalDate;
import java.util.List;
import ru.npi.kbju.application.observation.OperationTitle;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;

/** Открытый прикладной контракт, через который UI и proxy вызывают сценарии. */
public interface NutritionPlanOperations {
    /** Возвращает снимок планов; чтение не включено в счётчик предметных команд. */
    List<NutritionPlan> all();

    /** Проверяет и сохраняет новый план. */
    @OperationTitle("Создать план питания")
    NutritionPlan addPlan(String name, LocalDate effectiveFrom, NutritionPlan.Status status)
            throws DuplicatePlanException;

    /** Добавляет черновик с текущей датой. */
    NutritionPlan addDraft(String name) throws DuplicatePlanException;

    /** Удаляет план по устойчивому идентификатору. */
    boolean remove(long id);

    /** Изменяет состояние существующего плана. */
    @OperationTitle("Изменить состояние плана питания")
    NutritionPlan changeStatus(long id, NutritionPlan.Status newStatus);
}
