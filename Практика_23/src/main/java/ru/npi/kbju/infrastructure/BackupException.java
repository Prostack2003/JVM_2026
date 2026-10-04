package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.io.Serial;

/** Описывает точную границу отказа резервного копирования или восстановления. */
public final class BackupException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Стабильные причины, которые можно проверять без анализа текста исключения. */
    public enum Reason {
        SOURCE_NOT_FOUND,
        TARGET_EXISTS,
        SAME_PATH,
        INTEGRITY_FAILED,
        UNSUPPORTED_SCHEMA,
        SNAPSHOT_FAILED,
        RESTORE_FAILED
    }

    private final Reason reason;

    /** Создаёт отказ без технической причины нижнего уровня. */
    public BackupException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** Сохраняет исходную техническую причину для диагностики вызывающего кода. */
    public BackupException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /** Возвращает стабильную категорию отказа. */
    public Reason reason() {
        return reason;
    }
}
