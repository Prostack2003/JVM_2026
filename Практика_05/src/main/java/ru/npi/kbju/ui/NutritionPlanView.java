package ru.npi.kbju.ui;

import java.util.List;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;

/** Собирает первый экран и переводит действие пользователя в операцию сервиса. */
public final class NutritionPlanView {
    private final NutritionPlanService service;
    private final BorderPane root = new BorderPane();
    private final TableView<NutritionPlan> table;
    private final TextField nameField = new TextField();
    private final Button addButton = new Button("Добавить план");
    private final Label messageLabel = new Label();

    /**
     * Создаёт граф интерфейса для заданного сервиса текущего сеанса.
     *
     * @param service сервис планов питания
     */
    public NutritionPlanView(NutritionPlanService service) {
        this.service = service;
        this.table = createTable();
        configureForm();
        configureLayout();
    }

    /**
     * Возвращает корневой узел сцены.
     *
     * @return корневой контейнер
     */
    public Parent root() {
        return root;
    }

    private TableView<NutritionPlan> createTable() {
        var items = FXCollections.observableArrayList(service.plans());
        var result = new TableView<NutritionPlan>(items);
        result.setPlaceholder(new Label("Планы питания ещё не добавлены"));
        result.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        var idColumn = new TableColumn<NutritionPlan, Number>("ID");
        idColumn.setCellValueFactory(value -> new SimpleLongProperty(value.getValue().id()));
        idColumn.setMinWidth(70);
        idColumn.setMaxWidth(100);

        var nameColumn = new TableColumn<NutritionPlan, String>("Название плана");
        nameColumn.setCellValueFactory(
                value -> new SimpleStringProperty(value.getValue().name()));
        nameColumn.setMinWidth(190);

        var dateColumn = new TableColumn<NutritionPlan, String>("Дата начала");
        dateColumn.setCellValueFactory(
                value -> new SimpleStringProperty(value.getValue().effectiveFrom().toString()));
        dateColumn.setMinWidth(125);

        var statusColumn = new TableColumn<NutritionPlan, String>("Состояние");
        statusColumn.setCellValueFactory(
                value -> new SimpleStringProperty(statusText(value.getValue().status())));
        statusColumn.setMinWidth(135);

        result.getColumns().setAll(List.of(idColumn, nameColumn, dateColumn, statusColumn));
        return result;
    }

    private void configureForm() {
        nameField.setPromptText("Например, поддержание массы");
        nameField.setAccessibleText("Название нового плана питания");
        addButton.setDefaultButton(true);
        addButton.setOnAction(event -> addPlan());
        messageLabel.setText("Введите название нового плана");
        messageLabel.setWrapText(true);
    }

    private void configureLayout() {
        var title = new Label("Дневник питания КБЖУ");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        var subtitle = new Label("Планы питания текущего сеанса");
        var header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(20, 24, 12, 24));

        var formTitle = new Label("Новый план");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        var nameLabel = new Label("Название");
        var form = new VBox(10, formTitle, nameLabel, nameField, addButton, messageLabel);
        form.setPadding(new Insets(18));
        form.setPrefWidth(300);
        form.setMinWidth(240);

        BorderPane.setMargin(table, new Insets(0, 12, 20, 24));
        BorderPane.setMargin(form, new Insets(0, 24, 20, 0));
        root.setTop(header);
        root.setCenter(table);
        root.setRight(form);
    }

    private void addPlan() {
        try {
            var addedPlan = service.addDraft(nameField.getText());
            table.getItems().add(addedPlan);
            nameField.clear();
            messageLabel.setText("План «" + addedPlan.name() + "» добавлен");
        } catch (IllegalArgumentException exception) {
            messageLabel.setText("Введите непустое название плана питания");
            nameField.requestFocus();
        }
    }

    private static String statusText(NutritionPlan.Status status) {
        return switch (status) {
            case DRAFT -> "Черновик";
            case ACTIVE -> "Действует";
            case COMPLETED -> "Завершён";
            case NEEDS_REVIEW -> "Нужен пересмотр";
        };
    }

    int rowCount() {
        return table.getItems().size();
    }

    String messageText() {
        return messageLabel.getText();
    }

    String inputText() {
        return nameField.getText();
    }

    boolean controlsAvailable() {
        return table.isVisible()
                && table.getWidth() > 0
                && nameField.isVisible()
                && nameField.getWidth() > 0
                && addButton.isVisible()
                && addButton.getWidth() > 0;
    }

    void enterName(String value) {
        nameField.setText(value);
    }

    void submit() {
        addButton.fire();
    }
}
