package ru.npi.kbju.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/** Загружает внешние пути и разрешает относительные значения от корня данных. */
public record AppConfig(
        Path dataRoot,
        Path configFile,
        Source source,
        Path databasePath,
        Path logDirectory,
        Path backupDirectory
) {
    public static final String DATA_ROOT_PROPERTY = "kbju.data.dir";
    public static final String CONFIG_FILE_PROPERTY = "kbju.config";
    public static final String DATABASE_PATH_KEY = "database.path";
    public static final String LOG_DIRECTORY_KEY = "log.directory";
    public static final String BACKUP_DIRECTORY_KEY = "backup.directory";

    private static final String DEFAULT_DATABASE_PATH = "data/nutrition-plans.db";
    private static final String DEFAULT_LOG_DIRECTORY = "logs";
    private static final String DEFAULT_BACKUP_DIRECTORY = "backup";

    /** Источник применённых настроек. */
    public enum Source { EXTERNAL_FILE, DOCUMENTED_DEFAULTS }

    /** Проверяет компоненты и хранит только абсолютные нормализованные пути. */
    public AppConfig {
        dataRoot = requireAbsolute(dataRoot, DATA_ROOT_PROPERTY);
        configFile = requireAbsolute(configFile, CONFIG_FILE_PROPERTY);
        source = Objects.requireNonNull(source, "Источник конфигурации обязателен");
        databasePath = requireAbsolute(databasePath, DATABASE_PATH_KEY);
        logDirectory = requireAbsolute(logDirectory, LOG_DIRECTORY_KEY);
        backupDirectory = requireAbsolute(backupDirectory, BACKUP_DIRECTORY_KEY);
    }

    /** Загружает настройки из путей, выбранных системными свойствами. */
    public static AppConfig load() {
        var defaultRoot = Path.of(
                requireSystemProperty("user.home"), ".kbju-diary");
        var root = systemPath(DATA_ROOT_PROPERTY, defaultRoot);
        var file = systemPath(CONFIG_FILE_PROPERTY, Path.of("config/application.properties"));
        return load(root, file);
    }

    /** Загружает файл из явного учебного окружения без изменения системных настроек. */
    public static AppConfig load(Path dataRoot, Path configFile) {
        var root = requireAbsolute(dataRoot, DATA_ROOT_PROPERTY);
        var file = configFile.toAbsolutePath().normalize();
        var values = new Properties();
        var source = Source.DOCUMENTED_DEFAULTS;

        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                values.load(reader);
                source = Source.EXTERNAL_FILE;
            } catch (IOException exception) {
                throw new AppConfigurationException(
                        CONFIG_FILE_PROPERTY,
                        "Не удалось прочитать файл конфигурации",
                        exception
                );
            }
        }

        return new AppConfig(
                root,
                file,
                source,
                resolve(values, DATABASE_PATH_KEY, DEFAULT_DATABASE_PATH, root),
                resolve(values, LOG_DIRECTORY_KEY, DEFAULT_LOG_DIRECTORY, root),
                resolve(values, BACKUP_DIRECTORY_KEY, DEFAULT_BACKUP_DIRECTORY, root)
        );
    }

    /** Создаёт только каталоги изменяемых данных, но не саму базу или журнал. */
    public void prepareDirectories() {
        createDirectory(databasePath.getParent(), DATABASE_PATH_KEY);
        createDirectory(logDirectory, LOG_DIRECTORY_KEY);
        createDirectory(backupDirectory, BACKUP_DIRECTORY_KEY);
    }

    private static Path systemPath(String property, Path fallback) {
        var raw = System.getProperty(property);
        if (raw == null) {
            return fallback.toAbsolutePath().normalize();
        }
        if (raw.isBlank()) {
            throw new AppConfigurationException(property, "Системное свойство не может быть пустым");
        }
        try {
            return Path.of(raw).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new AppConfigurationException(property, "Системное свойство содержит неверный путь", exception);
        }
    }

    private static String requireSystemProperty(String key) {
        var value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new AppConfigurationException(key, "Обязательное системное свойство отсутствует");
        }
        return value;
    }

    private static Path resolve(Properties values, String key, String fallback, Path root) {
        var raw = values.getProperty(key, fallback).strip();
        if (raw.isBlank()) {
            throw new AppConfigurationException(key, "Настройка не может быть пустой");
        }
        try {
            var value = Path.of(raw);
            return (value.isAbsolute() ? value : root.resolve(value))
                    .toAbsolutePath()
                    .normalize();
        } catch (InvalidPathException exception) {
            throw new AppConfigurationException(key, "Настройка содержит неверный путь", exception);
        }
    }

    private static Path requireAbsolute(Path path, String key) {
        Objects.requireNonNull(path, "Путь обязателен: " + key);
        var normalized = path.toAbsolutePath().normalize();
        if (!normalized.isAbsolute()) {
            throw new AppConfigurationException(key, "Путь должен быть абсолютным");
        }
        return normalized;
    }

    private static void createDirectory(Path directory, String key) {
        if (directory == null) {
            throw new AppConfigurationException(key, "У пути отсутствует родительский каталог");
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new AppConfigurationException(
                    key,
                    "Не удалось подготовить каталог для настройки",
                    exception
            );
        }
    }
}
