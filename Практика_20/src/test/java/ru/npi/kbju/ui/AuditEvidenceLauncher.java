package ru.npi.kbju.ui;

import javafx.application.Application;

/** Запускает отдельный JavaFX-сценарий практики 13. */
public final class AuditEvidenceLauncher {
    private AuditEvidenceLauncher() {
    }

    /** Передаёт управление JavaFX Application и возвращает ошибку Gradle. */
    public static void main(String[] args) {
        Application.launch(AuditEvidenceCapture.class, args);
        if (AuditEvidenceCapture.failure() != null) {
            throw new IllegalStateException(
                    "UI-сценарий аудита завершился ошибкой",
                    AuditEvidenceCapture.failure());
        }
    }
}
