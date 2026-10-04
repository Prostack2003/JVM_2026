package ru.npi.kbju.domain;

import java.util.Objects;

/** Ожидаемое нарушение инварианта плана с указанием проблемного поля. */
public final class PlanValidationException extends IllegalArgumentException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /** Поле предметной записи, нарушившее инвариант. */
    public enum Field { ID, NAME, EFFECTIVE_FROM, STATUS }

    private final Field field;

    /** Создаёт диагностируемое нарушение предметного правила. */
    public PlanValidationException(Field field, String message) {
        super(message);
        this.field = Objects.requireNonNull(field, "Поле ошибки обязательно");
    }

    /** Возвращает поле, возле которого интерфейс может показать исправление. */
    public Field field() {
        return field;
    }
}
