package ru.npi.kbju.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Неизменяемый план питания с датой начала и состоянием жизненного цикла.
 *
 * @param id идентификатор; ноль допустим до сохранения, отрицательные значения запрещены
 * @param name понятное пользователю название плана
 * @param effectiveFrom дата начала действия плана
 * @param status текущее состояние плана
 */
public record NutritionPlan(
        long id,
        String name,
        LocalDate effectiveFrom,
        Status status
) {
    /** Допустимые состояния плана питания. */
    public enum Status {
        DRAFT,
        ACTIVE,
        COMPLETED,
        NEEDS_REVIEW
    }

    /** Проверяет инварианты и нормализует название при создании записи. */
    public NutritionPlan {
        if (id < 0) {
            throw new IllegalArgumentException("Идентификатор не может быть отрицательным");
        }
        name = requireName(name);
        Objects.requireNonNull(effectiveFrom, "Дата начала обязательна");
        Objects.requireNonNull(status, "Состояние обязательно");
    }

    /**
     * Возвращает новый план с другим состоянием, сохраняя остальные поля.
     *
     * @param newStatus новое состояние
     * @return новая запись плана питания
     */
    public NutritionPlan withStatus(Status newStatus) {
        return new NutritionPlan(id, name, effectiveFrom, newStatus);
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Название обязательно");
        }
        return value.trim();
    }
}
