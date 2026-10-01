package ru.npi.kbju.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.npi.kbju.application.NutritionPlanService;

/** Управляет жизненным циклом окна дневника питания. */
public final class DesktopApp extends Application {
    /** Создаёт и показывает главное окно в потоке JavaFX. */
    @Override
    public void start(Stage primaryStage) {
        var view = new NutritionPlanView(NutritionPlanService.createDefault());
        primaryStage.setTitle("Дневник питания КБЖУ");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(new Scene(view.root(), 1080, 640));
        primaryStage.show();

        if (getParameters().getRaw().contains("--smoke")) {
            Platform.runLater(() -> {
                System.out.println("JavaFX-окно показано");
                System.out.println("Начальное число строк: " + view.rowCount());
                System.out.println("Связанный счётчик: " + view.counterText());
                Platform.exit();
            });
        }
    }

    /**
     * Передаёт аргументы среде JavaFX.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        launch(args);
    }
}
