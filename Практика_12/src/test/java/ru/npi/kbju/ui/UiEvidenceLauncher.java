package ru.npi.kbju.ui;

import javafx.application.Application;

/** Запускает служебное JavaFX-приложение без специальной обработки Java launcher. */
public final class UiEvidenceLauncher {
    private UiEvidenceLauncher() {
    }

    /** Передаёт управление сценарию сохранения доказательств. */
    public static void main(String[] args) {
        Application.launch(UiEvidenceCapture.class, args);
        if (UiEvidenceCapture.failure() != null) {
            throw new IllegalStateException(
                    "Сценарий проверки JavaFX завершился ошибкой",
                    UiEvidenceCapture.failure()
            );
        }
    }
}
