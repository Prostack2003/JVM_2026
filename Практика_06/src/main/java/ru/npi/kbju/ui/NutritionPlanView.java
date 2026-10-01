package ru.npi.kbju.ui;

import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;

/** Собирает экран наблюдаемого списка планов и его связанные действия. */
public final class NutritionPlanView {
    private final NutritionPlanService service;
    private final BorderPane root = new BorderPane();
    private final TableView<NutritionPlan> table;
    private final TextField nameField = new TextField();
    private final Button addButton = new Button("Добавить план");
    private final Button activateButton = new Button("Сделать активным");
    private final Button removeButton = new Button("Удалить выбранный");
    private final Label countLabel = new Label();
    private final Label messageLabel = new Label();

    /**
     * Создаёт граф интерфейса поверх наблюдаемого списка сервиса.
     *
     * @param service сервис планов текущего сеанса
     */
    public NutritionPlanView(NutritionPlanService service) {
        this.service = service;
        this.table = createTable();
        configureBindings();
        configureActions();
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
        var result = new TableView<NutritionPlan>(service.plans());
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

    private void configureBindings() {
        countLabel.textProperty().bind(
                Bindings.size(service.plans()).asString("Планов: %d"));
        addButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> nameField.getText().isBlank(),
                nameField.textProperty()
        ));
        removeButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull());
        activateButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    var selected = table.getSelectionModel().getSelectedItem();
                    return selected == null || selected.status() == NutritionPlan.Status.ACTIVE;
                },
                table.getSelectionModel().selectedItemProperty()
        ));
    }

    private void configureActions() {
        nameField.setPromptText("Например, поддержание массы");
        nameField.setAccessibleText("Название нового плана питания");
        addButton.setDefaultButton(true);
        addButton.setOnAction(event -> addPlan());
        activateButton.setOnAction(event -> activateSelectedPlan());
        removeButton.setOnAction(event -> removeSelectedPlan());
        messageLabel.setText("Введите название или выберите строку таблицы");
        messageLabel.setWrapText(true);
    }

    private void configureLayout() {
        var title = new Label("Дневник питания КБЖУ");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        var subtitle = new Label("Наблюдаемые планы питания текущего сеанса");
        var header = new VBox(4, title, subtitle, countLabel);
        header.setPadding(new Insets(20, 24, 12, 24));

        var formTitle = new Label("Новый план");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        var nameLabel = new Label("Название");
        var selectionTitle = new Label("Выбранный план");
        selectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        var selectionActions = new VBox(10, selectionTitle, activateButton, removeButton);
        var form = new VBox(
                10,
                formTitle,
                nameLabel,
                nameField,
                addButton,
                selectionActions,
                messageLabel
        );
        form.setPadding(new Insets(18));
        form.setPrefWidth(310);
        form.setMinWidth(250);

        var tableArea = new VBox(8, table);
        tableArea.setPadding(new Insets(0, 12, 20, 24));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        var content = new HBox(tableArea, form);
        HBox.setHgrow(tableArea, javafx.scene.layout.Priority.ALWAYS);
        root.setTop(header);
        root.setCenter(content);
    }

    private void addPlan() {
        try {
            var addedPlan = service.addDraft(nameField.getText());
            nameField.clear();
            table.getSelectionModel().select(addedPlan);
            messageLabel.setText("План «" + addedPlan.name() + "» добавлен");
        } catch (IllegalArgumentException exception) {
            messageLabel.setText(exception.getMessage());
            nameField.requestFocus();
        }
    }

    private void removeSelectedPlan() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (service.remove(selected)) {
            table.getSelectionModel().clearSelection();
            messageLabel.setText("План «" + selected.name() + "» удалён");
        }
    }

    private void activateSelectedPlan() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        var replacement = service.changeStatus(selected, NutritionPlan.Status.ACTIVE);
        table.getSelectionModel().select(replacement);
        messageLabel.setText("План «" + replacement.name() + "» теперь действует");
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

    String counterText() {
        return countLabel.getText();
    }

    String messageText() {
        return messageLabel.getText();
    }

    String inputText() {
        return nameField.getText();
    }

    boolean addDisabled() {
        return addButton.isDisabled();
    }

    boolean removeDisabled() {
        return removeButton.isDisabled();
    }

    NutritionPlan selectedPlan() {
        return table.getSelectionModel().getSelectedItem();
    }

    NutritionPlan planAt(int index) {
        return table.getItems().get(index);
    }

    void enterName(String value) {
        nameField.setText(value);
    }

    void submit() {
        addButton.fire();
    }

    void selectRow(int index) {
        table.getSelectionModel().select(index);
    }

    void activateSelected() {
        activateButton.fire();
    }

    void removeSelected() {
        removeButton.fire();
    }
}
