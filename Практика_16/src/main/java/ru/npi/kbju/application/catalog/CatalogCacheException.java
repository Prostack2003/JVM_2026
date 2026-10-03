package ru.npi.kbju.application.catalog;

import java.util.Objects;

/** Точная причина отказа локального кэша. */
public final class CatalogCacheException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Вид отказа хранилища. */
    public enum Kind {
        NO_SAVED_DATA,
        CORRUPT_CACHE,
        READ_FAILED,
        WRITE_FAILED
    }

    private final Kind kind;

    /** Создаёт классифицированный отказ кэша. */
    public CatalogCacheException(Kind kind, String message) {
        this(kind, message, null);
    }

    /** Создаёт отказ кэша и сохраняет техническую причину. */
    public CatalogCacheException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "Вид отказа кэша обязателен");
    }

    /** Возвращает вид отказа. */
    public Kind kind() {
        return kind;
    }
}
