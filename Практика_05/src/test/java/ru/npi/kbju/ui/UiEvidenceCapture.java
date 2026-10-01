package ru.npi.kbju.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import ru.npi.kbju.application.NutritionPlanService;

/** Выполняет фактические JavaFX-сценарии и сохраняет их наблюдаемый результат. */
public final class UiEvidenceCapture extends Application {
    private static volatile Throwable failure;

    /** Создаёт окно, выполняет сценарии и завершает JavaFX после сохранения снимков. */
    @Override
    public void start(Stage stage) {
        var outputDirectory = Path.of(getParameters().getRaw().getFirst());
        var view = new NutritionPlanView(NutritionPlanService.createDefault());
        stage.setTitle("Дневник питания КБЖУ");
        stage.setMinWidth(860);
        stage.setMinHeight(460);
        stage.setScene(new Scene(view.root(), 1020, 600));
        stage.show();

        Platform.runLater(() -> {
            try {
                verifyAndCapture(stage, view, outputDirectory);
            } catch (Throwable throwable) {
                failure = throwable;
            } finally {
                Platform.exit();
            }
        });
    }

    private static void verifyAndCapture(
            Stage stage,
            NutritionPlanView view,
            Path outputDirectory
    ) throws IOException {
        Files.createDirectories(outputDirectory);
        require(view.rowCount() == 1, "Первый запуск должен содержать одну строку");
        System.out.println("Первый запуск: строк=1");

        view.enterName("Поддержание массы");
        view.submit();
        require(view.rowCount() == 2, "Одно нажатие должно добавить одну строку");
        require(view.messageText().contains("добавлен"), "Нужно подтверждение добавления");
        require(view.inputText().isEmpty(), "После успеха поле должно очиститься");
        capture(stage, outputDirectory.resolve("ui-success.png"));
        System.out.println("Успешный ввод: строк=2; " + view.messageText());

        view.enterName("   ");
        view.submit();
        require(view.rowCount() == 2, "Пустой ввод не должен менять таблицу");
        require(view.messageText().contains("непустое"), "Нужно объяснить исправление ввода");
        capture(stage, outputDirectory.resolve("ui-error.png"));
        System.out.println("Пустой ввод: строк=2; " + view.messageText());

        stage.setWidth(860);
        stage.setHeight(500);
        var root = (Region) stage.getScene().getRoot();
        root.resize(860, 500);
        root.applyCss();
        root.layout();
        require(view.controlsAvailable(), "Таблица и форма должны оставаться доступны");
        var rowsBeforeResizedSubmit = view.rowCount();
        view.enterName("Набор массы");
        view.submit();
        require(
                view.rowCount() == rowsBeforeResizedSubmit + 1,
                "Одно нажатие после изменения размера должно добавить одну строку"
        );
        capture(stage, outputDirectory.resolve("ui-resized.png"));
        System.out.println("Изменение размеров: элементы доступны; одно нажатие: строк=3");
    }

    private static void capture(Stage stage, Path output) throws IOException {
        var root = stage.getScene().getRoot();
        root.applyCss();
        root.layout();
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

    static Throwable failure() {
        return failure;
    }
}
