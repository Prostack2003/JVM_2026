package ru.npi.kbju.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.npi.kbju.application.NutritionPlanService;

/** Управляет жизненным циклом первого окна дневника питания. */
public final class DesktopApp extends Application {
    /** Создаёт и показывает главное окно в потоке JavaFX. */
    @Override
    public void start(Stage primaryStage) {
        var view = new NutritionPlanView(NutritionPlanService.createDefault());
        primaryStage.setTitle("Дневник питания КБЖУ");
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(460);
        primaryStage.setScene(new Scene(view.root(), 1020, 600));
        primaryStage.show();

        if (getParameters().getRaw().contains("--smoke")) {
            Platform.runLater(() -> {
                System.out.println("JavaFX-окно показано");
                System.out.println("Начальное число строк: " + view.rowCount());
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
