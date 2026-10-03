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
    private final Integer httpStatusCode;

    /** Создаёт отказ без нижестоящей причины. */
    public DirectoryLoadException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    /** Создаёт отказ и сохраняет техническую причину. */
    public DirectoryLoadException(Kind kind, String message, Throwable cause) {
        this(kind, message, null, cause);
    }

    /** Создаёт отказ HTTP с кодом для точной политики retry. */
    public static DirectoryLoadException httpStatus(int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("Код HTTP должен быть от 100 до 599");
        }
        return new DirectoryLoadException(
                Kind.HTTP_STATUS,
                "Сервер справочника вернул HTTP " + statusCode,
                statusCode,
                null);
    }

    private DirectoryLoadException(
            Kind kind,
            String message,
            Integer httpStatusCode,
            Throwable cause
    ) {
        super(message, cause);
        this.kind = java.util.Objects.requireNonNull(kind, "Вид отказа обязателен");
        this.httpStatusCode = httpStatusCode;
    }

    /** Возвращает класс отказа для точной диагностики. */
    public Kind kind() {
        return kind;
    }

    /** Возвращает HTTP-код или пустое значение для не-HTTP отказа. */
    public java.util.OptionalInt httpStatusCode() {
        return httpStatusCode == null
                ? java.util.OptionalInt.empty()
                : java.util.OptionalInt.of(httpStatusCode);
    }
}
