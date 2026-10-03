package ru.npi.kbju.infrastructure;

import java.nio.file.Path;

/** Вычисляет устойчивый путь пользовательских данных без абсолютной строки в исходнике. */
public final class ApplicationDataPaths {
    /** Системное свойство для явного выбора каталога данных при запуске и проверках. */
    public static final String DATA_DIRECTORY_PROPERTY = "kbju.data.dir";
    private static final String DATABASE_FILE_NAME = "nutrition-plans.db";

    private ApplicationDataPaths() {
    }

    /** Возвращает путь базы относительно настройки либо домашнего каталога пользователя. */
    public static Path databasePath() {
        var configured = System.getProperty(DATA_DIRECTORY_PROPERTY);
        var directory = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".kbju-diary")
                : Path.of(configured);
        return directory.toAbsolutePath().normalize().resolve(DATABASE_FILE_NAME);
    }
}
