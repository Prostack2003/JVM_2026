package ru.npi.kbju;

import ru.npi.kbju.ui.DesktopApp;

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
        DesktopApp.main(args);
    }
}
