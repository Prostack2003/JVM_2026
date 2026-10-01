/** Первый графический этап дневника питания КБЖУ. */
module ru.npi.kbju.desktop {
    requires java.desktop;
    requires javafx.controls;

    exports ru.npi.kbju;
    exports ru.npi.kbju.application;
    exports ru.npi.kbju.domain;
    exports ru.npi.kbju.ui to javafx.graphics;
}
