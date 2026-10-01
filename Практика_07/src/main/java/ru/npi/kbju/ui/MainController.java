package ru.npi.kbju.ui;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;

/** Связывает элементы FXML с прикладными операциями планов питания. */
public final class MainController {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.forLanguageTag("ru"));

    private final NutritionPlanService service;

    @FXML private TableView<NutritionPlan> planTable;
    @FXML private TableColumn<NutritionPlan, Number> idColumn;
    @FXML private TableColumn<NutritionPlan, String> nameColumn;
    @FXML private TableColumn<NutritionPlan, String> dateColumn;
    @FXML private TableColumn<NutritionPlan, String> statusColumn;
    @FXML private VBox formPanel;
    @FXML private Label nameLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private TextField nameField;
    @FXML private DatePicker effectiveFromPicker;
    @FXML private ComboBox<NutritionPlan.Status> statusBox;
    @FXML private Button addButton;
    @FXML private Button activateButton;
    @FXML private Button removeButton;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;

    /** Создаётся фабрикой FXMLLoader с уже готовым прикладным сервисом. */
    public MainController(NutritionPlanService service) {
        this.service = Objects.requireNonNull(service, "Сервис планов обязателен");
    }

    /** Настраивает элементы после внедрения всех полей FXML. */
    @FXML
    private void initialize() {
        requireInjectedFields();
        configureTable();
        configureForm();
        configureBindings();
    }

    private void requireInjectedFields() {
        Objects.requireNonNull(planTable, "FXML fx:id planTable не связан с контроллером");
        Objects.requireNonNull(idColumn, "FXML fx:id idColumn не связан с контроллером");
        Objects.requireNonNull(nameColumn, "FXML fx:id nameColumn не связан с контроллером");
        Objects.requireNonNull(dateColumn, "FXML fx:id dateColumn не связан с контроллером");
        Objects.requireNonNull(statusColumn, "FXML fx:id statusColumn не связан с контроллером");
        Objects.requireNonNull(formPanel, "FXML fx:id formPanel не связан с контроллером");
        Objects.requireNonNull(nameLabel, "FXML fx:id nameLabel не связан с контроллером");
        Objects.requireNonNull(dateLabel, "FXML fx:id dateLabel не связан с контроллером");
        Objects.requireNonNull(statusLabel, "FXML fx:id statusLabel не связан с контроллером");
        Objects.requireNonNull(nameField, "FXML fx:id nameField не связан с контроллером");
        Objects.requireNonNull(effectiveFromPicker,
                "FXML fx:id effectiveFromPicker не связан с контроллером");
        Objects.requireNonNull(statusBox, "FXML fx:id statusBox не связан с контроллером");
        Objects.requireNonNull(addButton, "FXML fx:id addButton не связан с контроллером");
        Objects.requireNonNull(activateButton,
                "FXML fx:id activateButton не связан с контроллером");
        Objects.requireNonNull(removeButton,
                "FXML fx:id removeButton не связан с контроллером");
        Objects.requireNonNull(countLabel, "FXML fx:id countLabel не связан с контроллером");
        Objects.requireNonNull(messageLabel,
                "FXML fx:id messageLabel не связан с контроллером");
    }

    private void configureTable() {
        planTable.setItems(service.plans());
        planTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        idColumn.setCellValueFactory(value -> new SimpleLongProperty(value.getValue().id()));
        nameColumn.setCellValueFactory(
                value -> new SimpleStringProperty(value.getValue().name()));
        dateColumn.setCellValueFactory(value -> new SimpleStringProperty(
                DATE_FORMAT.format(value.getValue().effectiveFrom())));
        statusColumn.setCellValueFactory(value -> new SimpleStringProperty(
                statusText(value.getValue().status())));
    }

    private void configureForm() {
        nameLabel.setLabelFor(nameField);
        dateLabel.setLabelFor(effectiveFromPicker);
        statusLabel.setLabelFor(statusBox);
        nameField.setAccessibleText("Название нового плана питания");
        effectiveFromPicker.setAccessibleText("Дата начала плана питания");
        statusBox.setAccessibleText("Исходное состояние плана питания");

        effectiveFromPicker.setValue(java.time.LocalDate.of(2026, 10, 1));
        statusBox.setItems(FXCollections.observableArrayList(NutritionPlan.Status.values()));
        statusBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(NutritionPlan.Status status) {
                return status == null ? "" : statusText(status);
            }

            @Override
            public NutritionPlan.Status fromString(String value) {
                throw new UnsupportedOperationException("Состояние выбирается из списка");
            }
        });
        statusBox.getSelectionModel().select(NutritionPlan.Status.DRAFT);
        messageLabel.setText("Заполните три поля и сохраните план");
    }

    private void configureBindings() {
        countLabel.textProperty().bind(Bindings.size(service.plans()).asString("Планов: %d"));
        addButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> nameField.getText().isBlank()
                        || effectiveFromPicker.getValue() == null
                        || statusBox.getValue() == null,
                nameField.textProperty(),
                effectiveFromPicker.valueProperty(),
                statusBox.valueProperty()
        ));
        removeButton.disableProperty().bind(
                planTable.getSelectionModel().selectedItemProperty().isNull());
        activateButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    var selected = planTable.getSelectionModel().getSelectedItem();
                    return selected == null || selected.status() == NutritionPlan.Status.ACTIVE;
                },
                planTable.getSelectionModel().selectedItemProperty()
        ));
    }

    /** Сохраняет значения трёх полей через прикладной сервис. */
    @FXML
    private void addPlan() {
        try {
            var added = service.addPlan(
                    nameField.getText(),
                    effectiveFromPicker.getValue(),
                    statusBox.getValue()
            );
            nameField.clear();
            planTable.getSelectionModel().select(added);
            messageLabel.setText("План «" + added.name() + "» сохранён");
        } catch (IllegalArgumentException | NullPointerException exception) {
            messageLabel.setText(exception.getMessage());
            nameField.requestFocus();
        }
    }

    /** Делает выбранный план активным. */
    @FXML
    private void activateSelectedPlan() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        var replacement = service.changeStatus(selected, NutritionPlan.Status.ACTIVE);
        planTable.getSelectionModel().select(replacement);
        messageLabel.setText("План «" + replacement.name() + "» теперь действует");
    }

    /** Удаляет выбранный план из текущего сеанса. */
    @FXML
    private void removeSelectedPlan() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        if (selected != null && service.remove(selected)) {
            planTable.getSelectionModel().clearSelection();
            messageLabel.setText("План «" + selected.name() + "» удалён");
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

    int boundFormFieldCount() {
        return (nameField == null ? 0 : 1)
                + (effectiveFromPicker == null ? 0 : 1)
                + (statusBox == null ? 0 : 1);
    }

    int rowCount() {
        return planTable.getItems().size();
    }

    String counterText() {
        return countLabel.getText();
    }

    String messageText() {
        return messageLabel.getText();
    }

    NutritionPlan planAt(int index) {
        return planTable.getItems().get(index);
    }

    void enterPlan(String name, java.time.LocalDate date, NutritionPlan.Status status) {
        nameField.setText(name);
        effectiveFromPicker.setValue(date);
        statusBox.setValue(status);
    }

    void submitPlan() {
        addButton.fire();
    }

    boolean formDoesNotOverlapTable() {
        Bounds tableBounds = planTable.localToScene(planTable.getLayoutBounds());
        Bounds formBounds = formPanel.localToScene(formPanel.getLayoutBounds());
        return tableBounds.getMaxX() <= formBounds.getMinX();
    }

    String layoutBounds() {
        Bounds tableBounds = planTable.localToScene(planTable.getLayoutBounds());
        Bounds formBounds = formPanel.localToScene(formPanel.getLayoutBounds());
        return "table.maxX=" + tableBounds.getMaxX()
                + "; form.minX=" + formBounds.getMinX();
    }
}
