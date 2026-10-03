/** Настольное приложение дневника питания с интерфейсом на FXML и CSS. */
module ru.npi.kbju.desktop {
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    requires transitive javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;

    exports ru.npi.kbju;
    exports ru.npi.kbju.application;
    exports ru.npi.kbju.application.audit;
    exports ru.npi.kbju.application.catalog;
    exports ru.npi.kbju.application.concurrency;
    exports ru.npi.kbju.application.importing;
    exports ru.npi.kbju.application.observation;
    exports ru.npi.kbju.domain;
    exports ru.npi.kbju.integration.cache;
    exports ru.npi.kbju.integration.http;
    exports ru.npi.kbju.lesson;
    exports ru.npi.kbju.ui to javafx.graphics;
    opens ru.npi.kbju.ui to javafx.fxml;
}
