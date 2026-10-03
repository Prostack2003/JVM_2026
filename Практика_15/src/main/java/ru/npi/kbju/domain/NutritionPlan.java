package ru.npi.kbju.domain;

import java.time.LocalDate;

/**
 * Неизменяемый план питания, защищающий допустимое состояние независимо от интерфейса.
 *
 * @param id идентификатор; ноль допустим до сохранения
 * @param name понятное пользователю название длиной не более 60 символов
 * @param effectiveFrom обязательная дата начала действия
 * @param status обязательное состояние жизненного цикла
 */
public record NutritionPlan(long id, String name, LocalDate effectiveFrom, Status status) {
    /** Максимальная длина названия, одинаковая для модели и формы. */
    public static final int MAX_NAME_LENGTH = 60;

    /** Допустимые состояния плана питания. */
    public enum Status { DRAFT, ACTIVE, COMPLETED, NEEDS_REVIEW }

    /** Проверяет инварианты и нормализует название при каждом создании записи. */
    public NutritionPlan {
        if (id < 0) {
            throw new PlanValidationException(
                    PlanValidationException.Field.ID,
                    "Идентификатор не может быть отрицательным");
        }
        name = requireName(name);
        if (effectiveFrom == null) {
            throw new PlanValidationException(
                    PlanValidationException.Field.EFFECTIVE_FROM,
                    "Дата начала обязательна");
        }
        if (status == null) {
            throw new PlanValidationException(
                    PlanValidationException.Field.STATUS,
                    "Состояние обязательно");
        }
    }

    /** Возвращает новую запись с другим состоянием, сохраняя остальные поля. */
    public NutritionPlan withStatus(Status newStatus) {
        return new NutritionPlan(id, name, effectiveFrom, newStatus);
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new PlanValidationException(
                    PlanValidationException.Field.NAME,
                    "Название плана обязательно");
        }
        var normalized = value.strip();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new PlanValidationException(
                    PlanValidationException.Field.NAME,
                    "Название должно быть не длиннее " + MAX_NAME_LENGTH + " символов");
        }
        return normalized;
    }
}
