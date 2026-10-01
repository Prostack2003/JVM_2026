package ru.npi.kbju.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.npi.kbju.ApplicationContext;
import ru.npi.kbju.application.NutritionPlanOperations;

/** Загружает интерфейс из classpath и управляет жизненным циклом окна. */
public final class DesktopApp extends Application {
    static final String FXML_RESOURCE = "/ru/npi/kbju/ui/main-view.fxml";
    static final String CSS_RESOURCE = "/ru/npi/kbju/ui/style.css";
    private ApplicationContext applicationContext;

    /** Создаёт и показывает главное окно в потоке JavaFX. */
    @Override
    public void start(Stage primaryStage) throws IOException {
        applicationContext = ApplicationContext.createDefault();
        var loadedView = loadMainView(applicationContext.service());
        var scene = new Scene(loadedView.root(), 1120, 720);
        scene.getStylesheets().add(loadedView.stylesheet());

        primaryStage.setTitle("Дневник питания КБЖУ");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setScene(scene);
        primaryStage.show();

        if (getParameters().getRaw().contains("--smoke")) {
            Platform.runLater(() -> {
                System.out.println("FXML загружен из classpath: " + FXML_RESOURCE);
                System.out.println("CSS загружен из classpath: " + CSS_RESOURCE);
                System.out.println("Связанных полей формы: "
                        + loadedView.controller().boundFormFieldCount());
                System.out.println("Начальное число строк: "
                        + loadedView.controller().rowCount());
                System.out.println("Контроллер получает данные через NutritionPlanOperations");
                System.out.println("Отмеченных предметных вызовов: "
                        + applicationContext.metrics().total());
                Platform.exit();
            });
        }
    }

    /** Закрывает выбранный адаптер хранилища при завершении приложения. */
    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    /**
     * Загружает основную разметку и передаёт сервис контроллеру через фабрику.
     *
     * @param service прикладной сервис текущего сеанса
     * @return корень, контроллер и URL таблицы стилей
     * @throws IOException если FXML не удалось загрузить
     */
    static LoadedView loadMainView(NutritionPlanOperations service) throws IOException {
        return loadView(requiredResource(FXML_RESOURCE), service);
    }

    /** Загружает указанную копию FXML тем же способом, что и рабочий экран. */
    static LoadedView loadView(URL fxml, NutritionPlanOperations service) throws IOException {
        Objects.requireNonNull(fxml, "URL FXML обязателен");
        Objects.requireNonNull(service, "Сервис планов обязателен");

        var loader = new FXMLLoader(fxml);
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(service);
            }
            throw new IllegalArgumentException("Неизвестный FXML-контроллер: " + type.getName());
        });
        Parent root = loader.load();
        MainController controller = loader.getController();
        var stylesheet = requiredResource(CSS_RESOURCE).toExternalForm();
        return new LoadedView(root, controller, stylesheet);
    }

    /** Возвращает обязательный classpath-ресурс или сообщает точный отсутствующий путь. */
    static URL requiredResource(String path) {
        return Objects.requireNonNull(
                DesktopApp.class.getResource(path),
                "Classpath-ресурс не найден: " + path
        );
    }

    /** Корень экрана и связанные объекты, полученные при одной загрузке FXML. */
    record LoadedView(Parent root, MainController controller, String stylesheet) {
        LoadedView {
            Objects.requireNonNull(root, "Корень FXML обязателен");
            Objects.requireNonNull(controller, "Контроллер FXML обязателен");
            Objects.requireNonNull(stylesheet, "URL CSS обязателен");
        }
    }

    /** Передаёт аргументы среде JavaFX. */
    public static void main(String[] args) {
        launch(args);
    }
}
