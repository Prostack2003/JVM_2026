package ru.npi.kbju.application.importing;

/** Ошибка формата CSV, найденная до открытия транзакции базы данных. */
public final class CsvImportException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    /** Создаёт диагностируемый отказ для конкретной строки входного файла. */
    public CsvImportException(int lineNumber, String message) {
        this(lineNumber, message, null);
    }

    /** Создаёт отказ и сохраняет причину преобразования значения. */
    public CsvImportException(int lineNumber, String message, Throwable cause) {
        super("Строка " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }

    /** Возвращает номер строки CSV, начиная с единицы. */
    public int lineNumber() {
        return lineNumber;
    }
}
