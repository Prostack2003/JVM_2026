package ru.npi.kbju.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.npi.kbju.application.DuplicatePlanException;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.PlanValidationException;

/** Координирует доступную форму, конкретные ошибки ввода и прикладной сервис. */
public final class MainController {
    private static final DateTimeFormatter TABLE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.forLanguageTag("ru"));
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

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
    @FXML private Label nameError;
    @FXML private TextField effectiveFromField;
    @FXML private Label dateError;
    @FXML private ComboBox<NutritionPlan.Status> statusBox;
    @FXML private Label statusError;
    @FXML private Button addButton;
    @FXML private Button activateButton;
    @FXML private Button removeButton;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;

    /** Создаётся фабрикой FXMLLoader с готовым сервисом. */
    public MainController(NutritionPlanService service) {
        this.service = Objects.requireNonNull(service, "Сервис планов обязателен");
    }

    /** Настраивает внедрённые элементы, но не сохраняет данные. */
    @FXML
    private void initialize() {
        requireInjectedFields();
        configureTable();
        configureForm();
        configureBindings();
        Platform.runLater(nameField::requestFocus);
    }

    private void requireInjectedFields() {
        Objects.requireNonNull(planTable, "FXML fx:id planTable не связан");
        Objects.requireNonNull(idColumn, "FXML fx:id idColumn не связан");
        Objects.requireNonNull(nameColumn, "FXML fx:id nameColumn не связан");
        Objects.requireNonNull(dateColumn, "FXML fx:id dateColumn не связан");
        Objects.requireNonNull(statusColumn, "FXML fx:id statusColumn не связан");
        Objects.requireNonNull(formPanel, "FXML fx:id formPanel не связан");
        Objects.requireNonNull(nameLabel, "FXML fx:id nameLabel не связан");
        Objects.requireNonNull(dateLabel, "FXML fx:id dateLabel не связан");
        Objects.requireNonNull(statusLabel, "FXML fx:id statusLabel не связан");
        Objects.requireNonNull(nameField, "FXML fx:id nameField не связан");
        Objects.requireNonNull(nameError, "FXML fx:id nameError не связан");
        Objects.requireNonNull(effectiveFromField, "FXML fx:id effectiveFromField не связан");
        Objects.requireNonNull(dateError, "FXML fx:id dateError не связан");
        Objects.requireNonNull(statusBox, "FXML fx:id statusBox не связан");
        Objects.requireNonNull(statusError, "FXML fx:id statusError не связан");
        Objects.requireNonNull(addButton, "FXML fx:id addButton не связан");
        Objects.requireNonNull(activateButton, "FXML fx:id activateButton не связан");
        Objects.requireNonNull(removeButton, "FXML fx:id removeButton не связан");
        Objects.requireNonNull(countLabel, "FXML fx:id countLabel не связан");
        Objects.requireNonNull(messageLabel, "FXML fx:id messageLabel не связан");
    }

    private void configureTable() {
        planTable.setItems(service.plans());
        planTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        idColumn.setCellValueFactory(value -> new SimpleLongProperty(value.getValue().id()));
        nameColumn.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().name()));
        dateColumn.setCellValueFactory(value -> new SimpleStringProperty(
                TABLE_DATE_FORMAT.format(value.getValue().effectiveFrom())));
        statusColumn.setCellValueFactory(value -> new SimpleStringProperty(
                statusText(value.getValue().status())));
    }

    private void configureForm() {
        nameLabel.setLabelFor(nameField);
        dateLabel.setLabelFor(effectiveFromField);
        statusLabel.setLabelFor(statusBox);
        nameField.setAccessibleText("Название нового плана питания, максимум 60 символов");
        effectiveFromField.setAccessibleText("Дата начала плана в формате год-месяц-день");
        statusBox.setAccessibleText("Исходное состояние плана питания");
        addButton.setAccessibleText("Сохранить новый план питания");
        messageLabel.setAccessibleText("Результат сохранения плана питания");

        nameError.managedProperty().bind(nameError.visibleProperty());
        dateError.managedProperty().bind(dateError.visibleProperty());
        statusError.managedProperty().bind(statusError.visibleProperty());
        effectiveFromField.setText("2026-10-01");
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
        clearErrors();
        messageLabel.setText("Заполните поля. Ошибка не удалит введённые значения");
    }

    private void configureBindings() {
        countLabel.textProperty().bind(Bindings.size(service.plans()).asString("Планов: %d"));
        removeButton.disableProperty().bind(
                planTable.getSelectionModel().selectedItemProperty().isNull());
        activateButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    var selected = planTable.getSelectionModel().getSelectedItem();
                    return selected == null || selected.status() == NutritionPlan.Status.ACTIVE;
                }, planTable.getSelectionModel().selectedItemProperty()));
    }

    /** Проверяет ввод и сохраняет только после успешного разбора всех значений. */
    @FXML
    private void addPlan() {
        clearErrors();

        var rawName = nameField.getText();
        if (rawName == null || rawName.isBlank()) {
            reject(nameField, nameError, "Введите название плана, а не только пробелы");
            return;
        }
        if (rawName.strip().length() > NutritionPlan.MAX_NAME_LENGTH) {
            reject(nameField, nameError,
                    "Сократите название до 60 символов или меньше");
            return;
        }

        var rawDate = effectiveFromField.getText();
        if (rawDate == null || rawDate.isBlank()) {
            reject(effectiveFromField, dateError,
                    "Введите дату в формате ГГГГ-ММ-ДД, например 2026-11-01");
            return;
        }

        final LocalDate effectiveFrom;
        try {
            effectiveFrom = LocalDate.parse(rawDate.strip(), INPUT_DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            reject(effectiveFromField, dateError,
                    "Дата не распознана. Используйте формат ГГГГ-ММ-ДД, например 2026-11-01");
            return;
        }

        if (statusBox.getValue() == null) {
            reject(statusBox, statusError, "Выберите состояние плана из списка");
            return;
        }

        try {
            var added = service.addPlan(rawName, effectiveFrom, statusBox.getValue());
            nameField.clear();
            planTable.getSelectionModel().select(added);
            messageLabel.setText("План «" + added.name() + "» сохранён");
            nameField.requestFocus();
        } catch (DuplicatePlanException exception) {
            reject(nameField, nameError, exception.getMessage());
        } catch (PlanValidationException exception) {
            showDomainError(exception);
        }
    }

    private void showDomainError(PlanValidationException exception) {
        switch (exception.field()) {
            case NAME -> reject(nameField, nameError, exception.getMessage());
            case EFFECTIVE_FROM -> reject(effectiveFromField, dateError, exception.getMessage());
            case STATUS -> reject(statusBox, statusError, exception.getMessage());
            case ID -> messageLabel.setText("Не удалось назначить идентификатор плана");
        }
    }

    private void reject(Node field, Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setAccessibleText("Ошибка: " + message);
        errorLabel.setVisible(true);
        field.pseudoClassStateChanged(INVALID, true);
        messageLabel.setText("Исправьте отмеченное поле. Остальные значения сохранены");
        field.requestFocus();
    }

    private void clearErrors() {
        clearError(nameField, nameError);
        clearError(effectiveFromField, dateError);
        clearError(statusBox, statusError);
    }

    private static void clearError(Node field, Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        field.pseudoClassStateChanged(INVALID, false);
    }

    /** Делает выбранный план активным. */
    @FXML
    private void activateSelectedPlan() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            var replacement = service.changeStatus(selected, NutritionPlan.Status.ACTIVE);
            planTable.getSelectionModel().select(replacement);
            messageLabel.setText("План «" + replacement.name() + "» теперь действует");
        }
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
                + (effectiveFromField == null ? 0 : 1)
                + (statusBox == null ? 0 : 1);
    }

    int rowCount() { return planTable.getItems().size(); }
    String counterText() { return countLabel.getText(); }
    String messageText() { return messageLabel.getText(); }
    NutritionPlan planAt(int index) { return planTable.getItems().get(index); }
    String nameErrorText() { return nameError.getText(); }
    String dateErrorText() { return dateError.getText(); }
    String statusErrorText() { return statusError.getText(); }
    String nameInput() { return nameField.getText(); }
    String dateInput() { return effectiveFromField.getText(); }
    NutritionPlan.Status statusInput() { return statusBox.getValue(); }
    boolean saveEnabled() { return !addButton.isDisabled(); }

    void enterPlan(String name, String date, NutritionPlan.Status status) {
        nameField.setText(name);
        effectiveFromField.setText(date);
        statusBox.setValue(status);
    }

    void setDateInput(String date) { effectiveFromField.setText(date); }
    void submitPlan() { addButton.fire(); }
    void requestNameFocus() { nameField.requestFocus(); }

    String focusedControlId() {
        var scene = nameField.getScene();
        var owner = scene == null ? null : scene.getFocusOwner();
        return owner == null ? "" : Objects.toString(owner.getId(), owner.getClass().getSimpleName());
    }

    void sendKey(KeyCode code, boolean shift) {
        var owner = nameField.getScene().getFocusOwner();
        if (owner == null) {
            throw new IllegalStateException("Нет владельца фокуса для клавиатурного события");
        }
        Event.fireEvent(owner, new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
        Event.fireEvent(owner, new KeyEvent(
                KeyEvent.KEY_RELEASED, "", "", code, shift, false, false, false));
    }

    void typeText(String text) {
        for (var index = 0; index < text.length(); index++) {
            var character = String.valueOf(text.charAt(index));
            var owner = nameField.getScene().getFocusOwner();
            Event.fireEvent(owner, new KeyEvent(
                    KeyEvent.KEY_TYPED, character, character, KeyCode.UNDEFINED,
                    false, false, false, false));
        }
    }

    boolean accessibilityContractIsComplete() {
        return nameLabel.getLabelFor() == nameField
                && dateLabel.getLabelFor() == effectiveFromField
                && statusLabel.getLabelFor() == statusBox
                && !nameField.getAccessibleText().isBlank()
                && !effectiveFromField.getAccessibleText().isBlank()
                && !statusBox.getAccessibleText().isBlank();
    }

    boolean formDoesNotOverlapTable() {
        Bounds tableBounds = planTable.localToScene(planTable.getLayoutBounds());
        Bounds formBounds = formPanel.localToScene(formPanel.getLayoutBounds());
        return tableBounds.getMaxX() <= formBounds.getMinX();
    }
}
