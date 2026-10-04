package ru.npi.kbju.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.npi.kbju.ApplicationContext;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.infrastructure.AppConfig;
import ru.npi.kbju.infrastructure.AppConfigurationException;
import ru.npi.kbju.infrastructure.ApplicationLog;
import ru.npi.kbju.domain.NutritionPlan;

/** Загружает интерфейс из classpath и управляет жизненным циклом окна. */
public final class DesktopApp extends Application {
    static final String FXML_RESOURCE = "/ru/npi/kbju/ui/main-view.fxml";
    static final String CSS_RESOURCE = "/ru/npi/kbju/ui/style.css";
    private ApplicationContext applicationContext;
    private ApplicationLog applicationLog;
    private AppConfig config;
    private static volatile boolean packageProbeFailed;

    /** Создаёт и показывает главное окно в потоке JavaFX. */
    @Override
    public void start(Stage primaryStage) {
        try {
            config = AppConfig.load();
            config.prepareDirectories();
        } catch (AppConfigurationException exception) {
            var eventId = "CONFIG-" + UUID.randomUUID();
            System.err.println("event=configuration_rejected eventId=" + eventId
                    + " key=" + exception.key());
            showStartupFailure(
                    primaryStage,
                    "Не удалось прочитать настройки. Проверьте параметр «"
                            + exception.key() + "» и перезапустите приложение.",
                    eventId
            );
            return;
        }

        final LoadedView loadedView;
        try {
            applicationLog = ApplicationLog.open(config.logDirectory());
            applicationLog.recordApplicationStarted(config);
            applicationContext = ApplicationContext.create(config.databasePath());
            loadedView = loadMainView(applicationContext.service());
        } catch (IOException | RuntimeException exception) {
            var eventId = applicationLog == null
                    ? "START-" + UUID.randomUUID()
                    : applicationLog.recordFailure(
                            ApplicationLog.Operation.APPLICATION_START,
                            ApplicationLog.FailureReason.UNEXPECTED_FAILURE,
                            exception
                    );
            showStartupFailure(
                    primaryStage,
                    "Не удалось открыть дневник. Проверьте доступ к каталогу данных и повторите запуск.",
                    eventId
            );
            return;
        }

        var scene = new Scene(loadedView.root(), 1180, 820);
        scene.getStylesheets().add(loadedView.stylesheet());

        primaryStage.setTitle("Дневник питания КБЖУ");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();

        var packageProbe = getParameters().getRaw().stream()
                .filter(argument -> argument.startsWith("--package-probe="))
                .findFirst();
        if (packageProbe.isPresent()) {
            Platform.runLater(() -> runPackageProbe(loadedView, scene));
        } else if (getParameters().getRaw().contains("--smoke")) {
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
                System.out.println("Источник настроек: " + config.source());
                System.out.println("Корень данных: " + config.dataRoot());
                System.out.println("Каталог журнала: " + applicationLog.directory());
                System.out.println("Файл SQLite: " + applicationContext.databasePath());
                Platform.exit();
            });
        }
    }

    /** Сообщает launcher, что автоматизированная проверка образа завершилась отказом. */
    public static boolean packageProbeFailed() {
        return packageProbeFailed;
    }

    private void runPackageProbe(LoadedView loadedView, Scene scene) {
        try {
            var mode = getParameters().getRaw().stream()
                    .filter(argument -> argument.startsWith("--package-probe="))
                    .findFirst()
                    .orElseThrow()
                    .substring("--package-probe=".length());
            scene.getRoot().applyCss();
            if (scene.getStylesheets().size() != 1) {
                throw new IllegalStateException("Таблица стилей не подключена");
            }
            switch (mode) {
                case "add" -> {
                    var before = loadedView.controller().rowCount();
                    loadedView.controller().enterPlan(
                            "Приёмка app-image",
                            "2026-10-22",
                            NutritionPlan.Status.DRAFT
                    );
                    loadedView.controller().submitPlan();
                    var after = loadedView.controller().rowCount();
                    if (before < 1 || after != before + 1) {
                        throw new IllegalStateException(
                                "Запись не добавлена через форму: до=" + before + ", после=" + after);
                    }
                    System.out.println("Проверка app-image: запись добавлена через форму; строк=" + after);
                }
                case "verify" -> {
                    if (loadedView.controller().rowCount() != 2) {
                        throw new IllegalStateException(
                                "Повторный запуск не нашёл сохранённую запись");
                    }
                    System.out.println("Проверка app-image: сохранённая запись найдена; строк=2");
                }
                default -> throw new IllegalArgumentException(
                        "Неизвестный режим проверки app-image: " + mode);
            }
            System.out.println("FXML: " + FXML_RESOURCE);
            System.out.println("CSS: " + CSS_RESOURCE);
            System.out.println("Стили сцены: " + scene.getStylesheets().size());
            System.out.println("Java runtime: " + System.getProperty("java.home"));
            System.out.println("Корень данных: " + config.dataRoot());
            System.out.println("Файл SQLite: " + applicationContext.databasePath());
        } catch (RuntimeException exception) {
            packageProbeFailed = true;
            System.err.println("Проверка app-image не пройдена: " + exception.getMessage());
        } finally {
            Platform.exit();
        }
    }

    /** Закрывает выбранный адаптер хранилища при завершении приложения. */
    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        if (applicationLog != null) {
            applicationLog.close();
        }
    }

    private void showStartupFailure(Stage stage, String action, String eventId) {
        var title = new Label("Дневник питания не запущен");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        var message = new Label(action + "\nКод события: " + eventId);
        message.setWrapText(true);
        message.setAccessibleText(action + " Код события " + eventId);
        var root = new VBox(14, title, message);
        root.setPadding(new Insets(24));
        stage.setTitle("Ошибка запуска — дневник питания КБЖУ");
        stage.setScene(new Scene(root, 640, 220));
        stage.show();
        if (getParameters().getRaw().contains("--smoke")) {
            Platform.runLater(Platform::exit);
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
