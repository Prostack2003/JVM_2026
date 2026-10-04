package ru.npi.kbju.domain;

/** Ожидаемый предметный отказ при попытке сохранить повторное название плана. */
public final class DuplicatePlanException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String planName;

    /** Создаёт ошибку для уже занятого нормализованного названия. */
    public DuplicatePlanException(String planName) {
        super("План «" + planName + "» уже существует. Измените название");
        this.planName = planName;
    }

    /** Возвращает конфликтующее название. */
    public String planName() {
        return planName;
    }
}
