package ru.npi.kbju.application;

/** Проверяемая ожидаемая ошибка попытки сохранить повторяющееся название плана. */
public final class DuplicatePlanException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String planName;

    /** Создаёт ошибку для уже существующего нормализованного названия. */
    public DuplicatePlanException(String planName) {
        super("План «" + planName + "» уже существует. Измените название");
        this.planName = planName;
    }

    /** Возвращает конфликтующее название. */
    public String planName() {
        return planName;
    }
}
