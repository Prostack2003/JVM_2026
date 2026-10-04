package ru.npi.kbju.application.concurrency;

/** Сообщает о неудачном ожидании параллельной диагностики и сохраняет причину. */
public final class ParallelDiagnosticException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Сохраняет исходное исключение из Future. */
    public ParallelDiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }
}
