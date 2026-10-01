package ru.npi.kbju.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет фактические FXML/JavaFX-сценарии и сохраняет снимки. */
public final class UiEvidenceCapture extends Application {
    private static volatile Throwable failure;

    /** Загружает рабочий FXML, выполняет проверки и завершает JavaFX после снимков. */
    @Override
    public void start(Stage stage) throws IOException {
        var outputDirectory = Path.of(getParameters().getRaw().getFirst());
        var loaded = DesktopApp.loadMainView(NutritionPlanService.createDefault());
        var scene = new Scene(loaded.root(), 1120, 680);
        scene.getStylesheets().add(loaded.stylesheet());
        stage.setTitle("Дневник питания КБЖУ — проверка практики 07");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            try {
                verifyInitialAndSaved(stage, loaded.controller(), outputDirectory);
                stage.setWidth(900);
                stage.setHeight(560);
            } catch (Throwable throwable) {
                failAndExit(throwable);
                return;
            }
            Platform.runLater(() -> verifyCompactThenExpanded(
                    stage, loaded.controller(), outputDirectory));
        });
    }

    private static void verifyInitialAndSaved(
            Stage stage,
            MainController controller,
            Path outputDirectory
    ) throws IOException {
        Files.createDirectories(outputDirectory);
        require(controller.boundFormFieldCount() == 3,
                "FXMLLoader должен связать все три поля формы");
        require(controller.rowCount() == 1, "Первый запуск должен содержать одну строку");
        require("Планов: 1".equals(controller.counterText()),
                "Связанный счётчик должен быть равен одному");
        require(!stage.getScene().getStylesheets().isEmpty(),
                "CSS должен быть подключён к сцене через classpath URL");
        layout(stage);
        require(controller.formDoesNotOverlapTable(),
                "Форма не должна перекрывать таблицу в исходном размере");
        capture(stage, outputDirectory.resolve("ui-initial.png"));
        System.out.println("FXML: связаны 3 поля; CSS подключён; строк=1");

        controller.enterPlan(
                "Поддержание массы",
                LocalDate.of(2026, 11, 1),
                NutritionPlan.Status.ACTIVE
        );
        controller.submitPlan();
        require(controller.rowCount() == 2, "Сохранение должно добавить одну строку");
        var saved = controller.planAt(1);
        require("Поддержание массы".equals(saved.name()), "Название должно прийти из формы");
        require(LocalDate.of(2026, 11, 1).equals(saved.effectiveFrom()),
                "Дата должна прийти из формы");
        require(saved.status() == NutritionPlan.Status.ACTIVE,
                "Состояние должно прийти из формы");
        require(controller.messageText().contains("сохранён"),
                "Контроллер должен показать результат действия");
        capture(stage, outputDirectory.resolve("ui-saved.png"));
        System.out.println("Сохранение: название, дата и состояние переданы сервису; строк=2");
    }

    private static void verifyCompactThenExpanded(
            Stage stage,
            MainController controller,
            Path outputDirectory
    ) {
        try {
            layout(stage);
            require(stage.getScene().getWidth() < 1000,
                    "Сцена должна фактически уменьшиться перед компактным снимком");
            System.out.println("Компактные границы: " + controller.layoutBounds());
            require(controller.formDoesNotOverlapTable(),
                    "При минимальном размере форма не должна перекрывать таблицу");
            capture(stage, outputDirectory.resolve("ui-compact.png"));
            System.out.printf("Компактное окно %.0fx%.0f: форма не перекрывает таблицу%n",
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setWidth(1280);
            stage.setHeight(760);
        } catch (Throwable throwable) {
            failAndExit(throwable);
            return;
        }
        Platform.runLater(() -> verifyExpanded(
                stage, controller, outputDirectory, 5));
    }

    private static void verifyExpanded(
            Stage stage,
            MainController controller,
            Path outputDirectory,
            int remainingPulses
    ) {
        layout(stage);
        if (stage.getScene().getWidth() <= 1200 && remainingPulses > 0) {
            Platform.runLater(() -> verifyExpanded(
                    stage, controller, outputDirectory, remainingPulses - 1));
            return;
        }
        try {
            require(stage.getScene().getWidth() > 1200,
                    "Сцена должна фактически увеличиться перед широким снимком");
            require(controller.formDoesNotOverlapTable(),
                    "При увеличении окна форма не должна перекрывать таблицу");
            capture(stage, outputDirectory.resolve("ui-expanded.png"));
            System.out.printf("Широкое окно %.0fx%.0f: таблица растёт, форма отдельна%n",
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            verifyBrokenFxId();
        } catch (Throwable throwable) {
            failure = throwable;
        } finally {
            Platform.exit();
        }
    }

    private static void verifyBrokenFxId() throws IOException {
        var resource = DesktopApp.requiredResource(DesktopApp.FXML_RESOURCE);
        String original;
        try (var stream = resource.openStream()) {
            original = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var broken = original.replace(
                "fx:id=\"nameField\"",
                "fx:id=\"brokenNameField\""
        );
        require(!broken.equals(original), "Проверочная копия должна изменить один fx:id");
        var copy = Files.createTempFile("kbju-broken-fx-id-", ".fxml");
        try {
            Files.writeString(copy, broken, StandardCharsets.UTF_8);
            try {
                DesktopApp.loadView(copy.toUri().toURL(), NutritionPlanService.createDefault());
                throw new AssertionError("Повреждённый fx:id должен приводить к отказу загрузки");
            } catch (IOException exception) {
                require(messageChain(exception).contains("nameField"),
                        "Ошибка должна указывать на несвязанное поле nameField");
                System.out.println(
                        "Повреждённый fx:id: ошибка воспроизведена; рабочий FXML не изменён");
            }
        } finally {
            Files.deleteIfExists(copy);
        }
    }

    private static String messageChain(Throwable throwable) {
        var result = new StringBuilder();
        for (var current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                result.append(current.getMessage()).append('\n');
            }
        }
        return result.toString();
    }

    private static void layout(Stage stage) {
        var root = stage.getScene().getRoot();
        root.applyCss();
        root.layout();
    }

    private static void capture(Stage stage, Path output) throws IOException {
        layout(stage);
        var root = stage.getScene().getRoot();
        var image = new WritableImage(
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getWidth())),
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getHeight()))
        );
        root.snapshot(null, image);
        var bufferedImage = new BufferedImage(
                (int) image.getWidth(),
                (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        var pixelReader = image.getPixelReader();
        for (var y = 0; y < bufferedImage.getHeight(); y++) {
            for (var x = 0; x < bufferedImage.getWidth(); x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        ImageIO.write(bufferedImage, "png", output.toFile());
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

    static Throwable failure() {
        return failure;
    }
}
