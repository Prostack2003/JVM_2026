package ru.npi.kbju.infrastructure;

import java.util.Objects;

/** Сообщает о конкретной некорректной настройке без публикации её значения. */
public final class AppConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String key;

    /** Создаёт диагностируемый отказ для указанного ключа. */
    public AppConfigurationException(String key, String message) {
        super(message);
        this.key = Objects.requireNonNull(key, "Ключ настройки обязателен");
    }

    /** Создаёт диагностируемый отказ и сохраняет техническую причину. */
    public AppConfigurationException(String key, String message, Throwable cause) {
        super(message, cause);
        this.key = Objects.requireNonNull(key, "Ключ настройки обязателен");
    }

    /** Возвращает безопасное имя проблемного параметра. */
    public String key() {
        return key;
    }
}
