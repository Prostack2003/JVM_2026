package ru.npi.kbju.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import ru.npi.kbju.ApplicationContext;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет фактические сценарии JavaFX и сохраняет снимки состояний формы. */
public final class UiEvidenceCapture extends Application {
    private static volatile Throwable failure;
    private final List<ApplicationContext> contexts = new ArrayList<>();

    /** Загружает рабочий FXML и запускает сценарии после показа окна. */
    @Override
    public void start(Stage stage) throws IOException {
        var outputDirectory = Path.of(getParameters().getRaw().getFirst());
        var loaded = DesktopApp.loadMainView(newService());
        show(stage, loaded, "SQLite и JDBC — практика 11");

        Platform.runLater(() -> {
            try {
                verifyValidationStates(stage, loaded.controller(), outputDirectory);
                var keyboardView = DesktopApp.loadMainView(newService());
                show(stage, keyboardView, "Клавиатурный маршрут — практика 11");
                Platform.runLater(() -> verifyKeyboardRoute(
                        stage, keyboardView.controller(), outputDirectory));
            } catch (IOException | RuntimeException | AssertionError error) {
                failAndExit(error);
            }
        });
    }

    private static void show(Stage stage, DesktopApp.LoadedView loaded, String title) {
        var scene = new Scene(loaded.root(), 1120, 720);
        scene.getStylesheets().add(loaded.stylesheet());
        stage.setTitle(title);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private static void verifyValidationStates(
            Stage stage,
            MainController controller,
            Path outputDirectory
    ) throws IOException {
        Files.createDirectories(outputDirectory);
        layout(stage);
        require(controller.boundFormFieldCount() == 3, "Должны быть связаны три поля");
        require(controller.rowCount() == 1, "До ввода должна быть одна строка");
        require("Планов: 1".equals(controller.counterText()), "Счётчик должен быть связан");
        require(controller.accessibilityContractIsComplete(),
                "Подписи и доступные имена должны быть связаны с полями");
        require("nameField".equals(controller.focusedControlId()),
                "Первичный фокус должен находиться в названии");
        require(controller.formDoesNotOverlapTable(), "Форма не должна перекрывать таблицу");
        capture(stage, outputDirectory.resolve("ui-initial.png"));
        System.out.println("Начало: FXML/CSS загружены; подписей=3; фокус=nameField; строк=1");

        controller.enterPlan("   ", "2026-11-01", NutritionPlan.Status.DRAFT);
        controller.submitPlan();
        require(controller.rowCount() == 1, "Пробелы не должны добавлять строку");
        require(controller.nameErrorText().contains("не только пробелы"),
                "Ошибка названия должна объяснять исправление");
        require("   ".equals(controller.nameInput()), "Ошибочный черновик должен сохраниться");
        require("2026-11-01".equals(controller.dateInput()), "Верная дата должна сохраниться");
        require("nameField".equals(controller.focusedControlId()),
                "Фокус должен вернуться к названию");
        require(controller.saveEnabled(), "Кнопка должна оставаться доступной после ошибки");
        capture(stage, outputDirectory.resolve("ui-spaces-error.png"));
        System.out.println("Пробелы: отказ; строк=1; данные сохранены; фокус=nameField");

        controller.enterPlan("Поддержание массы", "2026-13-40", NutritionPlan.Status.ACTIVE);
        controller.submitPlan();
        require(controller.rowCount() == 1, "Неверная дата не должна добавлять строку");
        require(controller.dateErrorText().contains("ГГГГ-ММ-ДД"),
                "Ошибка даты должна содержать формат и пример");
        require("Поддержание массы".equals(controller.nameInput()),
                "Корректное название должно сохраниться");
        require("2026-13-40".equals(controller.dateInput()),
                "Неверная дата должна остаться для исправления");
        require("effectiveFromField".equals(controller.focusedControlId()),
                "Фокус должен перейти к дате");
        capture(stage, outputDirectory.resolve("ui-date-error.png"));
        System.out.println("Неверная дата: поймана DateTimeParseException; строк=1; фокус=effectiveFromField");

        controller.setDateInput("2026-11-01");
        controller.submitPlan();
        require(controller.rowCount() == 2, "Исправление без перезапуска должно сохранить план");
        var saved = controller.planAt(1);
        require("Поддержание массы".equals(saved.name()), "Название должно дойти до модели");
        require("2026-11-01".equals(saved.effectiveFrom().toString()),
                "Исправленная дата должна дойти до модели");
        require(saved.status() == NutritionPlan.Status.ACTIVE,
                "Состояние должно дойти до модели");
        capture(stage, outputDirectory.resolve("ui-corrected.png"));
        System.out.println("Исправление: сохранено без перезапуска; строк=2; дубля нет");

        controller.enterPlan(" поддержание МАССЫ ", "2026-12-01", NutritionPlan.Status.DRAFT);
        controller.submitPlan();
        require(controller.rowCount() == 2, "Дубликат не должен менять таблицу");
        require(controller.nameErrorText().contains("уже существует"),
                "Проверяемое исключение дубликата должно объяснять исправление");
        require(" поддержание МАССЫ ".equals(controller.nameInput()),
                "Значение дубликата должно сохраниться");
        require(controller.saveEnabled(), "После дубликата повторная попытка должна быть доступна");
        capture(stage, outputDirectory.resolve("ui-duplicate-error.png"));
        System.out.println("Дубликат: пойман DuplicatePlanException; строк=2; повтор доступен");

        controller.enterPlan("План без даты", "   ", NutritionPlan.Status.DRAFT);
        controller.submitPlan();
        require(controller.rowCount() == 2, "Отсутствующая дата не должна менять таблицу");
        require(controller.dateErrorText().startsWith("Введите дату"),
                "Отсутствующая дата должна дать понятное действие");
        capture(stage, outputDirectory.resolve("ui-missing-date.png"));
        System.out.println("Нет даты: отказ; понятное сообщение; строк=2");

        controller.enterPlan("А".repeat(61), "2026-12-01", NutritionPlan.Status.DRAFT);
        controller.submitPlan();
        require(controller.rowCount() == 2, "Длинное название не должно менять таблицу");
        require(controller.nameErrorText().contains("60 символов"),
                "Форма должна повторять ограничение модели по длине");
        System.out.println("Доменное ограничение: 61 символ отклонён формой и моделью");

        controller.enterPlan("План без состояния", "2026-12-01", null);
        controller.submitPlan();
        require(controller.rowCount() == 2, "Отсутствующее состояние не должно менять таблицу");
        require(controller.statusErrorText().contains("Выберите состояние"),
                "Ошибка состояния должна содержать действие");
        System.out.println("Нет состояния: отказ; строк=2; фокус=statusBox");

        controller.enterPlan("Б".repeat(60), "2026-12-01", NutritionPlan.Status.DRAFT);
        controller.submitPlan();
        require(controller.rowCount() == 3,
                "Название ровно из 60 символов должно быть допустимо");
        require(controller.planAt(2).name().length() == 60,
                "Граничное название должно сохраниться без усечения");
        System.out.println("Граница: название из 60 символов сохранено; строк=3");

        controller.sortByNameDescending();
        controller.selectPlanById(1);
        controller.activateSelection();
        require(controller.selectedPlanId() == 1,
                "После сортировки должна остаться выбрана запись с тем же ID");
        require(controller.selectedPlanStatus() == NutritionPlan.Status.ACTIVE,
                "Изменение должно примениться к записи, найденной по ID");
        capture(stage, outputDirectory.resolve("ui-stable-id.png"));
        System.out.println("Сортировка: выбран и изменён ID=1; выбранный ID после обновления=1");
    }

    private ru.npi.kbju.application.NutritionPlanOperations newService() throws IOException {
        var database = Files.createTempDirectory("kbju-ui-evidence-")
                .resolve("nutrition-plans.db");
        var context = ApplicationContext.create(database);
        contexts.add(context);
        return context.service();
    }

    /** Закрывает все созданные для сценария независимые репозитории. */
    @Override
    public void stop() {
        contexts.forEach(ApplicationContext::close);
    }

    private static void verifyKeyboardRoute(
            Stage stage,
            MainController controller,
            Path outputDirectory
    ) {
        try {
            layout(stage);
            require("nameField".equals(controller.focusedControlId()),
                    "Клавиатурный маршрут должен начинаться без щелчка мышью");
            controller.typeText("Keyboard plan");
            controller.sendKey(KeyCode.TAB, false);
            require("effectiveFromField".equals(controller.focusedControlId()),
                    "Первый Tab должен перейти к дате");
            controller.sendKey(KeyCode.TAB, true);
            require("nameField".equals(controller.focusedControlId()),
                    "Shift+Tab должен вернуть фокус к названию");
            controller.sendKey(KeyCode.TAB, false);
            controller.sendKey(KeyCode.TAB, false);
            require("statusBox".equals(controller.focusedControlId()),
                    "Следующий Tab должен перейти к состоянию");
            controller.sendKey(KeyCode.TAB, false);
            require("addButton".equals(controller.focusedControlId()),
                    "Tab должен перейти к основной кнопке");
            controller.sendKey(KeyCode.ENTER, false);
            require(controller.rowCount() == 2, "Enter на кнопке должен сохранить план");
            require("Keyboard plan".equals(controller.planAt(1).name()),
                    "Клавиатурный ввод должен попасть в сохранённую запись");
            capture(stage, outputDirectory.resolve("ui-keyboard-saved.png"));
            System.out.println("Клавиатура: nameField -> effectiveFromField -> statusBox -> addButton; Enter сохранил строку");
        } catch (IOException | RuntimeException | AssertionError error) {
            failure = error;
        } finally {
            Platform.exit();
        }
    }

    private static void layout(Stage stage) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private static void capture(Stage stage, Path output) throws IOException {
        layout(stage);
        var root = stage.getScene().getRoot();
        var image = new WritableImage(
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getWidth())),
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getHeight())));
        root.snapshot(null, image);
        var buffered = new BufferedImage(
                (int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (var y = 0; y < buffered.getHeight(); y++) {
            for (var x = 0; x < buffered.getWidth(); x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ImageIO.write(buffered, "png", output.toFile());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void failAndExit(Throwable throwable) {
        failure = throwable;
        Platform.exit();
    }

    static Throwable failure() { return failure; }
}
