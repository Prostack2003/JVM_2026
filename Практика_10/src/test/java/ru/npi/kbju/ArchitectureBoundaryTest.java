package ru.npi.kbju;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Фиксирует направление зависимостей простым анализом импортов исходного кода. */
class ArchitectureBoundaryTest {
    private static final Path SOURCES = Path.of("src/main/java/ru/npi/kbju");

    @Test
    void domainDoesNotKnowJavaFxOrOuterLayers() throws IOException {
        assertNoImports("domain", "javafx.", "ru.npi.kbju.application",
                "ru.npi.kbju.infrastructure", "ru.npi.kbju.ui");
    }

    @Test
    void applicationDependsOnDomainButNotUiOrInfrastructure() throws IOException {
        assertNoImports("application", "javafx.", "ru.npi.kbju.infrastructure",
                "ru.npi.kbju.ui");
    }

    @Test
    void infrastructureImplementsDomainPortWithoutUi() throws IOException {
        assertNoImports("infrastructure", "javafx.", "ru.npi.kbju.application",
                "ru.npi.kbju.ui");
    }

    @Test
    void controllerDoesNotKnowConcreteRepository() throws IOException {
        var controller = Files.readString(SOURCES.resolve("ui/MainController.java"));
        assertFalse(controller.contains("ru.npi.kbju.infrastructure"));
        assertFalse(controller.contains("NutritionPlanService"));
        assertTrue(controller.contains("NutritionPlanOperations"));
    }

    private static void assertNoImports(String packageName, String... forbidden)
            throws IOException {
        try (var files = Files.walk(SOURCES.resolve(packageName))) {
            for (var path : files.filter(value -> value.toString().endsWith(".java")).toList()) {
                var source = Files.readString(path);
                for (var prefix : forbidden) {
                    assertFalse(source.contains("import " + prefix),
                            () -> path + " содержит запрещённый импорт " + prefix);
                }
            }
        }
    }
}
