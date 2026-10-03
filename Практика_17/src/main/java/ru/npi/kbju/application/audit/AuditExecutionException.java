package ru.npi.kbju.application.audit;

/** Сообщает, что фоновая проверка не может завершиться с достоверным результатом. */
public final class AuditExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Создаёт отказ с понятной причиной. */
    public AuditExecutionException(String message) {
        super(message);
    }
}
