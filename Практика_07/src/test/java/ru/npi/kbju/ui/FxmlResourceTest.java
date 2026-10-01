package ru.npi.kbju.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Проверяет контракт classpath-ресурсов до фактической JavaFX-загрузки. */
class FxmlResourceTest {
    @Test
    void fxmlContainsControllerThreeFieldsAndSaveHandler() throws IOException {
        var resource = DesktopApp.class.getResource(DesktopApp.FXML_RESOURCE);
        assertNotNull(resource, "FXML должен находиться через classpath");
        var xml = read(DesktopApp.FXML_RESOURCE);
        assertTrue(xml.contains("fx:controller=\"ru.npi.kbju.ui.MainController\""));
        assertTrue(xml.contains("fx:id=\"nameField\""));
        assertTrue(xml.contains("fx:id=\"effectiveFromPicker\""));
        assertTrue(xml.contains("fx:id=\"statusBox\""));
        assertTrue(xml.contains("onAction=\"#addPlan\""));
        assertFalse(xml.contains("src/main/resources"));
    }

    @Test
    void cssUsesSemanticSelectorsAndContainsNoBusinessRules() throws IOException {
        var resource = DesktopApp.class.getResource(DesktopApp.CSS_RESOURCE);
        assertNotNull(resource, "CSS должен находиться через classpath");
        var css = read(DesktopApp.CSS_RESOURCE);
        assertTrue(css.contains(".primary-button"));
        assertTrue(css.contains(".form-panel"));
        assertTrue(css.contains(".message-label"));
        assertFalse(css.contains("NutritionPlan"));
    }

    private static String read(String path) throws IOException {
        try (var stream = DesktopApp.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Classpath-ресурс должен входить в сборку: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
