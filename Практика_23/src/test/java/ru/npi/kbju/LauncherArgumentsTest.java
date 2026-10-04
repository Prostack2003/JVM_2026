package ru.npi.kbju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.infrastructure.AppConfig;

/** Проверяет явный путь данных, используемый в приёмке app-image. */
class LauncherArgumentsTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(AppConfig.DATA_ROOT_PROPERTY);
    }

    @Test
    void dataDirectoryArgumentSetsApplicationProperty() {
        Launcher.configureDataDirectory(new String[]{"--data-dir=/tmp/kbju-package-data"});

        assertEquals("/tmp/kbju-package-data",
                System.getProperty(AppConfig.DATA_ROOT_PROPERTY));
    }

    @Test
    void blankOrRepeatedDataDirectoryIsRejectedBeforeJavaFxStarts() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> Launcher.configureDataDirectory(new String[]{"--data-dir="}));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> Launcher.configureDataDirectory(new String[]{
                        "--data-dir=/tmp/one", "--data-dir=/tmp/two"
                }));
    }
}
