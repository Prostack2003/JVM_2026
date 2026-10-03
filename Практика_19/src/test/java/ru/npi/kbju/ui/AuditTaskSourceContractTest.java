package ru.npi.kbju.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Фиксирует границу между Task.call и визуальными узлами JavaFX. */
class AuditTaskSourceContractTest {
    private static final Path TASK_SOURCE = Path.of(
            "src/main/java/ru/npi/kbju/ui/NutritionPlanAuditTask.java");
    private static final Path CONTROLLER_SOURCE = Path.of(
            "src/main/java/ru/npi/kbju/ui/MainController.java");

    @Test
    void taskUsesObservableBridgeWithoutTouchingControls() throws IOException {
        var source = Files.readString(TASK_SOURCE);

        assertTrue(source.contains("extends Task<AuditResult>"));
        assertTrue(source.contains("List.copyOf(plans)"));
        assertTrue(source.contains("updateProgress"));
        assertTrue(source.contains("updateMessage"));
        assertTrue(source.contains("this::isCancelled"));
        assertFalse(source.contains("javafx.scene.control"));
        assertFalse(source.contains("Platform.runLater"));
    }

    @Test
    void controllerHandlesEveryTerminalStateAndCreatesNewTask() throws IOException {
        var source = Files.readString(CONTROLLER_SOURCE);

        assertTrue(source.contains("new NutritionPlanAuditTask"));
        assertTrue(source.contains("setOnSucceeded"));
        assertTrue(source.contains("setOnCancelled"));
        assertTrue(source.contains("setOnFailed"));
        assertTrue(source.contains("Thread.ofVirtual()"));
        assertTrue(source.contains("List.copyOf(visiblePlans)"));
    }
}
