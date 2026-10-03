package ru.npi.kbju.domain;

import java.util.List;
import java.util.Optional;

/** Порт хранения, описанный в терминах предметной области, а не памяти или SQL. */
public interface NutritionPlanRepository extends AutoCloseable {
    /** Возвращает неизменяемый снимок всех планов. */
    List<NutritionPlan> findAll();

    /** Ищет план по устойчивому идентификатору. */
    Optional<NutritionPlan> findById(long id);

    /** Создаёт или обновляет план и возвращает фактически сохранённое значение. */
    NutritionPlan save(NutritionPlan plan) throws DuplicatePlanException;

    /** Удаляет план с заданным идентификатором. */
    boolean deleteById(long id);

    /** Адаптер без внешних ресурсов наследует no-op; JDBC-адаптер закрывает соединение. */
    @Override
    default void close() {
    }
}
