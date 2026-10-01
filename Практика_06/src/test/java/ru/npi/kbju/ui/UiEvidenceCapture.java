package ru.npi.kbju.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет фактические JavaFX-сценарии и сохраняет наблюдаемые результаты. */
public final class UiEvidenceCapture extends Application {
    private static volatile Throwable failure;

    /** Создаёт окно, выполняет сценарии и завершает JavaFX после снимков. */
    @Override
    public void start(Stage stage) {
        var outputDirectory = Path.of(getParameters().getRaw().getFirst());
        var view = new NutritionPlanView(NutritionPlanService.createDefault());
        stage.setTitle("Дневник питания КБЖУ");
        stage.setMinWidth(900);
        stage.setMinHeight(500);
        stage.setScene(new Scene(view.root(), 1080, 640));
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
        require("Планов: 1".equals(view.counterText()), "Счётчик должен быть равен одному");
        require(view.removeDisabled(), "Без выбора удаление должно быть недоступно");
        require(view.addDisabled(), "При пустом поле добавление должно быть недоступно");
        capture(stage, outputDirectory.resolve("ui-initial.png"));
        System.out.println("Начальное состояние: строк=1; Планов: 1; удаление недоступно");

        view.enterName("Поддержание массы");
        require(!view.addDisabled(), "После ввода добавление должно стать доступно");
        view.submit();
        require(view.rowCount() == 2, "Добавление должно создать одну строку");
        require("Планов: 2".equals(view.counterText()), "Счётчик должен стать равен двум");
        require(view.inputText().isEmpty(), "После добавления поле должно очиститься");
        capture(stage, outputDirectory.resolve("ui-added.png"));
        System.out.println("Добавление: строк=2; Планов: 2");

        view.selectRow(0);
        var original = view.selectedPlan();
        view.activateSelected();
        var replacement = view.selectedPlan();
        require(replacement != null, "После замены строка должна оставаться выбранной");
        require(replacement != original, "Состояние меняется заменой неизменяемой записи");
        require(replacement.status() == NutritionPlan.Status.ACTIVE,
                "В таблице должно отображаться новое состояние");
        require(view.rowCount() == 2, "Замена не должна менять число строк");
        capture(stage, outputDirectory.resolve("ui-replaced.png"));
        System.out.println("Замена записи: строк=2; состояние=Действует");

        view.removeSelected();
        require(view.rowCount() == 1, "Удаление должно уменьшить таблицу на одну строку");
        require("Планов: 1".equals(view.counterText()), "Счётчик должен уменьшиться");
        require(view.selectedPlan() == null, "Выделение не должно ссылаться на удалённый план");
        require(view.removeDisabled(), "После удаления без выбора кнопка должна отключиться");
        capture(stage, outputDirectory.resolve("ui-deleted.png"));
        System.out.println("Удаление: строк=1; Планов: 1; выделение очищено");

        view.enterName(" поддержание МАССЫ ");
        view.submit();
        require(view.rowCount() == 1, "Дубликат не должен менять наблюдаемый список");
        require(view.messageText().contains("уже существует"),
                "Отказ должен объяснять причину");
        capture(stage, outputDirectory.resolve("ui-duplicate.png"));
        System.out.println("Дубликат: строк=1; показано объяснение отказа");

        view.selectRow(0);
        view.removeSelected();
        require(view.rowCount() == 0, "Последнюю строку можно удалить");
        require("Планов: 0".equals(view.counterText()), "Пустой список должен дать ноль");
        require(view.removeDisabled(), "В пустом списке удаление должно быть недоступно");
        capture(stage, outputDirectory.resolve("ui-empty.png"));
        System.out.println("Пустой список: строк=0; Планов: 0; удаление недоступно");
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
