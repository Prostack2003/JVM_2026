package ru.npi.kbju.domain;

/** Ожидаемый отказ всего пакета с указанием строки, на которой остановилась БД. */
public final class BatchImportException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String problemCode;

    /** Сохраняет проблемный код и исходную техническую причину. */
    public BatchImportException(String problemCode, Throwable cause) {
        super("Импорт пакета отменён на коде «" + problemCode
                + "». Ни одна строка пакета не сохранена", cause);
        this.problemCode = problemCode;
    }

    /** Возвращает код CSV-строки, обработка которой завершилась отказом. */
    public String problemCode() {
        return problemCode;
    }
}
