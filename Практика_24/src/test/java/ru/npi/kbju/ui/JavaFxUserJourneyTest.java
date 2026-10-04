package ru.npi.kbju.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Проверяет критический пользовательский маршрут через реальные FXML-элементы. */
class JavaFxUserJourneyTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    @BeforeAll
    static void startJavaFx() throws Exception {
        var started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
            assertTrue(started.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                    "JavaFX toolkit не запустился за отведённое время");
        } catch (IllegalStateException alreadyStarted) {
            Platform.setImplicitExit(false);
        }
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void userCanAddPlanThroughRealFxmlControls() throws Exception {
        var repository = new MemoryNutritionPlanRepository();
        var loaded = loadView(repository);

        onFxThread(() -> {
            Parent root = loaded.root();
            required(root, "#nameField", TextField.class).setText("Баланс на неделю");
            required(root, "#effectiveFromField", TextField.class).setText("2026-11-01");
            statusBox(root).setValue(NutritionPlan.Status.ACTIVE);
            required(root, "#addButton", Button.class).fire();
            return null;
        });

        onFxThread(() -> {
            var table = planTable(loaded.root());
            assertEquals(1, table.getItems().size());
            var saved = table.getItems().getFirst();
            assertTrue(saved.id() > 0);
            assertEquals("Баланс на неделю", saved.name());
            assertEquals("2026-11-01", saved.effectiveFrom().toString());
            assertEquals(NutritionPlan.Status.ACTIVE, saved.status());
            assertTrue(required(loaded.root(), "#messageLabel", Label.class)
                    .getText().contains("сохранён"));
            return null;
        });

        assertEquals(repository.findAll(), onFxThread(() ->
                planTable(loaded.root()).getItems().stream().toList()));
    }

    @Test
    void blankNameShowsFieldErrorAndDoesNotSave() throws Exception {
        var repository = new MemoryNutritionPlanRepository();
        var loaded = loadView(repository);

        onFxThread(() -> {
            Parent root = loaded.root();
            required(root, "#nameField", TextField.class).setText("   ");
            required(root, "#effectiveFromField", TextField.class).setText("2026-11-01");
            statusBox(root).setValue(NutritionPlan.Status.DRAFT);
            required(root, "#addButton", Button.class).fire();
            return null;
        });

        onFxThread(() -> {
            var error = required(loaded.root(), "#nameError", Label.class);
            assertTrue(error.isVisible());
            assertTrue(error.getText().contains("не только пробелы"));
            assertTrue(planTable(loaded.root()).getItems().isEmpty());
            return null;
        });
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void initialAuditMessageUsesCompleteWord() throws Exception {
        var loaded = loadView(new MemoryNutritionPlanRepository());

        assertEquals(
                "Подтверждённый результат отсутствует",
                onFxThread(loaded.controller()::auditResultText));
    }

    private static DesktopApp.LoadedView loadView(MemoryNutritionPlanRepository repository)
            throws Exception {
        return onFxThread(() -> {
            var loaded = DesktopApp.loadMainView(new NutritionPlanService(repository));
            var scene = new Scene(loaded.root());
            scene.getStylesheets().add(loaded.stylesheet());
            loaded.root().applyCss();
            return loaded;
        });
    }

    @SuppressWarnings("unchecked")
    private static TableView<NutritionPlan> planTable(Parent root) {
        return (TableView<NutritionPlan>) required(root, "#planTable", TableView.class);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<NutritionPlan.Status> statusBox(Parent root) {
        return (ComboBox<NutritionPlan.Status>) required(root, "#statusBox", ComboBox.class);
    }

    private static <T> T required(Parent root, String selector, Class<T> type) {
        var node = root.lookup(selector);
        assertNotNull(node, "В FXML отсутствует элемент " + selector);
        assertTrue(type.isInstance(node), selector + " имеет неожиданный тип");
        return type.cast(node);
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        var result = new AtomicReference<T>();
        var failure = new AtomicReference<Throwable>();
        var completed = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                "Операция в JavaFX Application Thread не завершилась вовремя");
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }
}
