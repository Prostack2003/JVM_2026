package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Пишет структурированные диагностические события по разрешённому набору полей. */
public final class ApplicationLog implements AutoCloseable {
    private static final int ROTATION_LIMIT_BYTES = 256 * 1024;
    private static final int ROTATION_FILE_COUNT = 3;

    /** Операции, разрешённые в безопасном диагностическом контексте. */
    public enum Operation { APPLICATION_START, CONFIGURATION_LOAD, PLAN_SAVE }

    /** Причины, которые можно записывать без произвольного пользовательского текста. */
    public enum FailureReason {
        INVALID_CONFIGURATION,
        STORAGE_UNAVAILABLE,
        UNEXPECTED_FAILURE
    }

    private final Logger logger;
    private final FileHandler handler;
    private final Path directory;

    private ApplicationLog(Logger logger, FileHandler handler, Path directory) {
        this.logger = logger;
        this.handler = handler;
        this.directory = directory;
    }

    /** Создаёт каталог и открывает журнал с ограниченной ротацией. */
    public static ApplicationLog open(Path directory) {
        Objects.requireNonNull(directory, "Каталог журнала обязателен");
        var normalized = directory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            var pattern = normalized.resolve("application-%g.log").toString();
            var handler = new FileHandler(
                    pattern, ROTATION_LIMIT_BYTES, ROTATION_FILE_COUNT, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.ALL);

            var logger = Logger.getLogger("kbju-diary-" + UUID.randomUUID());
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.INFO);
            logger.addHandler(handler);
            return new ApplicationLog(logger, handler, normalized);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось открыть журнал приложения", exception);
        }
    }

    /** Фиксирует запуск без абсолютных путей и содержимого пользовательских записей. */
    public void recordApplicationStarted(AppConfig config) {
        Objects.requireNonNull(config, "Конфигурация обязательна");
        write(Level.INFO, "event=application_started result=success configSource="
                + config.source().name());
    }

    /**
     * Фиксирует отказ и возвращает идентификатор для сообщения пользователю.
     * Текст и stack trace причины сознательно не записываются.
     */
    public String recordFailure(
            Operation operation,
            FailureReason reason,
            Throwable ignoredSensitiveCause
    ) {
        Objects.requireNonNull(operation, "Операция обязательна");
        Objects.requireNonNull(reason, "Причина обязательна");
        Objects.requireNonNull(ignoredSensitiveCause, "Техническая причина обязательна");
        var eventId = UUID.randomUUID().toString();
        write(Level.WARNING, "event=operation_failed eventId=" + eventId
                + " operation=" + operation.name()
                + " reason=" + reason.name());
        return eventId;
    }

    /** Возвращает фактический каталог журналов для диагностики запуска. */
    public Path directory() {
        return directory;
    }

    private void write(Level level, String message) {
        logger.log(level, message);
        handler.flush();
    }

    /** Закрывает файловый обработчик и освобождает lock-файл. */
    @Override
    public void close() {
        handler.close();
        logger.removeHandler(handler);
    }
}
