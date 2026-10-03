package ru.npi.kbju.application.catalog;

import java.util.Objects;

/** Объясняет, почему ни сеть, ни кэш не дали допустимый каталог. */
public final class CatalogUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Наблюдаемое состояние автономной загрузки. */
    public enum Kind {
        NO_SAVED_DATA,
        CORRUPT_CACHE,
        CACHE_READ_FAILED,
        EXPIRED_CACHE,
        CLOCK_ROLLBACK,
        UPDATE_IN_PROGRESS
    }

    private final Kind kind;

    /** Создаёт прикладной отказ и сохраняет исходную причину. */
    public CatalogUnavailableException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "Вид отказа обязателен");
    }

    /** Возвращает вид отказа. */
    public Kind kind() {
        return kind;
    }
}
