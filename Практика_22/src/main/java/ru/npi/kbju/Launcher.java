package ru.npi.kbju;

import java.util.Objects;
import ru.npi.kbju.ui.DesktopApp;
import ru.npi.kbju.infrastructure.AppConfig;

/** Обычная точка входа, передающая управление жизненному циклу JavaFX. */
public final class Launcher {
    private Launcher() {
    }

    /**
     * Запускает настольное приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        configureDataDirectory(args);
        DesktopApp.main(args);
        if (DesktopApp.packageProbeFailed()) {
            System.exit(2);
        }
    }

    /** Применяет явный каталог данных для воспроизводимой проверки поставки. */
    static void configureDataDirectory(String[] args) {
        Objects.requireNonNull(args, "Аргументы запуска обязательны");
        String selected = null;
        for (var argument : args) {
            if (argument.startsWith("--data-dir=")) {
                if (selected != null) {
                    throw new IllegalArgumentException("Каталог данных указан больше одного раза");
                }
                selected = argument.substring("--data-dir=".length());
            }
        }
        if (selected != null) {
            if (selected.isBlank()) {
                throw new IllegalArgumentException("Каталог данных не может быть пустым");
            }
            System.setProperty(AppConfig.DATA_ROOT_PROPERTY, selected);
        }
    }
}
