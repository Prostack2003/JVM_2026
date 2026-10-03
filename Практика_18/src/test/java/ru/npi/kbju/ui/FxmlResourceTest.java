package ru.npi.kbju.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Проверяет статический контракт FXML, CSS и границы обработки исключений. */
class FxmlResourceTest {
    @Test
    void packagedFxmlDeclaresControllerAndPrimaryHandler() throws IOException {
        var xml = read(DesktopApp.FXML_RESOURCE);
        assertTrue(xml.contains("fx:controller=\"ru.npi.kbju.ui.MainController\""));
        assertTrue(xml.contains("fx:id=\"addButton\" id=\"addButton\""));
        assertTrue(xml.contains("onAction=\"#addPlan\""));
    }

    @Test
    void missingClasspathResourceIsDiagnosable() {
        var failure = assertThrowsExactly(
                NullPointerException.class,
                () -> DesktopApp.requiredResource("/ru/npi/kbju/ui/missing-view.fxml")
        );

        assertTrue(failure.getMessage().contains("missing-view.fxml"));
    }

    @Test
    void fxmlContainsThreeInputsInlineErrorsLabelsAndDefaultAction() throws IOException {
        var xml = read(DesktopApp.FXML_RESOURCE);
        assertTrue(xml.contains("fx:id=\"nameField\""));
        assertTrue(xml.contains("fx:id=\"effectiveFromField\""));
        assertTrue(xml.contains("fx:id=\"statusBox\""));
        assertTrue(xml.contains("fx:id=\"nameError\""));
        assertTrue(xml.contains("fx:id=\"dateError\""));
        assertTrue(xml.contains("defaultButton=\"true\""));
        assertTrue(xml.contains("mnemonicParsing=\"true\""));
        assertFalse(xml.contains("DatePicker"));
    }

    @Test
    void cssAddsTextualAndVisualErrorState() throws IOException {
        var css = read(DesktopApp.CSS_RESOURCE);
        assertTrue(css.contains(".field-error"));
        assertTrue(css.contains(":invalid"));
        assertTrue(css.contains("-fx-border-color"));
    }

    @Test
    void fxmlContainsBackgroundAuditProgressCancellationAndFailureDemo() throws IOException {
        var xml = read(DesktopApp.FXML_RESOURCE);
        assertTrue(xml.contains("fx:id=\"auditProgressBar\""));
        assertTrue(xml.contains("fx:id=\"startAuditButton\""));
        assertTrue(xml.contains("fx:id=\"cancelAuditButton\""));
        assertTrue(xml.contains("onAction=\"#startAudit\""));
        assertTrue(xml.contains("onAction=\"#cancelAudit\""));
        assertTrue(xml.contains("fx:id=\"controlledFailureCheck\""));
        assertTrue(xml.contains("fx:id=\"auditResultLabel\""));
    }

    private static String read(String path) throws IOException {
        try (var stream = DesktopApp.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Classpath-ресурс должен входить в сборку: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
