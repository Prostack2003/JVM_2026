package ru.npi.kbju.application.catalog;

/** Классифицирует отказ на границе внешнего справочника. */
public final class DirectoryLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Наблюдаемые классы отказа. */
    public enum Kind {
        HTTP_STATUS,
        MALFORMED_JSON,
        CONTRACT,
        TIMEOUT,
        TRANSPORT,
        INTERRUPTED
    }

    private final Kind kind;

    /** Создаёт отказ без нижестоящей причины. */
    public DirectoryLoadException(Kind kind, String message) {
        super(message);
        this.kind = java.util.Objects.requireNonNull(kind, "Вид отказа обязателен");
    }

    /** Создаёт отказ и сохраняет техническую причину. */
    public DirectoryLoadException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = java.util.Objects.requireNonNull(kind, "Вид отказа обязателен");
    }

    /** Возвращает класс отказа для точной диагностики. */
    public Kind kind() {
        return kind;
    }
}
