package ru.npi.kbju.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.application.audit.AuditResult;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.PlanValidationException;

/** Координирует доступную форму, конкретные ошибки ввода и прикладной сервис. */
public final class MainController {
    private static final DateTimeFormatter TABLE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.forLanguageTag("ru"));
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final NutritionPlanOperations service;
    private final ObservableList<NutritionPlan> visiblePlans =
            FXCollections.observableArrayList();

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
    @FXML private ProgressBar auditProgressBar;
    @FXML private Label auditStatusLabel;
    @FXML private Label auditResultLabel;
    @FXML private Button startAuditButton;
    @FXML private Button cancelAuditButton;
    @FXML private CheckBox controlledFailureCheck;
    private NutritionPlanAuditTask currentAuditTask;
    private int auditRunCount;

    /** Создаётся фабрикой FXMLLoader с готовым сервисом. */
    public MainController(NutritionPlanOperations service) {
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
        Objects.requireNonNull(auditProgressBar, "FXML fx:id auditProgressBar не связан");
        Objects.requireNonNull(auditStatusLabel, "FXML fx:id auditStatusLabel не связан");
        Objects.requireNonNull(auditResultLabel, "FXML fx:id auditResultLabel не связан");
        Objects.requireNonNull(startAuditButton, "FXML fx:id startAuditButton не связан");
        Objects.requireNonNull(cancelAuditButton, "FXML fx:id cancelAuditButton не связан");
        Objects.requireNonNull(controlledFailureCheck,
                "FXML fx:id controlledFailureCheck не связан");
    }

    private void configureTable() {
        planTable.setItems(visiblePlans);
        planTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        idColumn.setCellValueFactory(value -> new SimpleLongProperty(value.getValue().id()));
        nameColumn.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().name()));
        dateColumn.setCellValueFactory(value -> new SimpleStringProperty(
                TABLE_DATE_FORMAT.format(value.getValue().effectiveFrom())));
        statusColumn.setCellValueFactory(value -> new SimpleStringProperty(
                statusText(value.getValue().status())));
        refreshAndSelect(null);
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
        auditProgressBar.setProgress(0);
        auditStatusLabel.setText("Аудит ещё не запускался");
        auditResultLabel.setText("Подтверждённый результат отсутствует");
        startAuditButton.setDisable(false);
        cancelAuditButton.setDisable(true);
    }

    private void configureBindings() {
        countLabel.textProperty().bind(Bindings.size(visiblePlans).asString("Планов: %d"));
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
            refreshAndSelect(added.id());
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
            var replacement = service.changeStatus(selected.id(), NutritionPlan.Status.ACTIVE);
            refreshAndSelect(replacement.id());
            messageLabel.setText("План «" + replacement.name() + "» теперь действует");
        }
    }

    /** Удаляет выбранный план из текущего сеанса. */
    @FXML
    private void removeSelectedPlan() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        if (selected != null && service.remove(selected.id())) {
            refreshAndSelect(null);
            planTable.getSelectionModel().clearSelection();
            messageLabel.setText("План «" + selected.name() + "» удалён");
        }
    }

    /** Обновляет снимок представления и восстанавливает выбор по устойчивому ID. */
    private void refreshAndSelect(Long selectedId) {
        visiblePlans.setAll(service.all());
        if (!planTable.getSortOrder().isEmpty()) {
            planTable.sort();
        }
        if (selectedId != null) {
            for (var index = 0; index < visiblePlans.size(); index++) {
                if (visiblePlans.get(index).id() == selectedId) {
                    planTable.getSelectionModel().select(index);
                    break;
                }
            }
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

    /** Запускает новую одноразовую Task для неизменяемого снимка таблицы. */
    @FXML
    private void startAudit() {
        if (currentAuditTask != null && currentAuditTask.isRunning()) {
            return;
        }

        var task = new NutritionPlanAuditTask(
                List.copyOf(visiblePlans), controlledFailureCheck.isSelected());
        currentAuditTask = task;
        auditRunCount++;
        auditProgressBar.progressProperty().bind(task.progressProperty());
        auditStatusLabel.textProperty().bind(task.messageProperty());
        startAuditButton.setDisable(true);
        cancelAuditButton.setDisable(false);
        controlledFailureCheck.setDisable(true);

        task.setOnSucceeded(event -> finishAudit(task, AuditOutcome.SUCCEEDED));
        task.setOnCancelled(event -> finishAudit(task, AuditOutcome.CANCELLED));
        task.setOnFailed(event -> finishAudit(task, AuditOutcome.FAILED));
        Thread.ofVirtual().name("kbju-plan-audit-" + auditRunCount).start(task);
    }

    /** Передаёт фоновой задаче запрос кооперативной отмены. */
    @FXML
    private void cancelAudit() {
        if (currentAuditTask != null) {
            currentAuditTask.cancel(true);
        }
    }

    private void finishAudit(NutritionPlanAuditTask task, AuditOutcome outcome) {
        if (task != currentAuditTask) {
            return;
        }
        auditProgressBar.progressProperty().unbind();
        auditStatusLabel.textProperty().unbind();
        startAuditButton.setDisable(false);
        cancelAuditButton.setDisable(true);
        controlledFailureCheck.setDisable(false);

        switch (outcome) {
            case SUCCEEDED -> {
                AuditResult result = task.getValue();
                auditProgressBar.setProgress(1);
                auditStatusLabel.setText("Аудит успешно завершён");
                auditResultLabel.setText("Подтверждено: " + result.checkedPlans()
                        + " план(а), активных " + result.activePlans()
                        + ", требуют пересмотра " + result.plansNeedingReview());
            }
            case CANCELLED -> auditStatusLabel.setText(
                    "Аудит отменён; неполный результат не опубликован");
            case FAILED -> auditStatusLabel.setText(
                    "Ошибка аудита: " + readableFailure(task.getException()));
        }
        currentAuditTask = null;
    }

    private static String readableFailure(Throwable failure) {
        if (failure == null || failure.getMessage() == null || failure.getMessage().isBlank()) {
            return "неизвестная причина; данные не изменены";
        }
        return failure.getMessage();
    }

    private enum AuditOutcome { SUCCEEDED, CANCELLED, FAILED }

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
    boolean auditRunning() { return currentAuditTask != null && currentAuditTask.isRunning(); }
    boolean auditStartEnabled() { return !startAuditButton.isDisabled(); }
    boolean auditCancelEnabled() { return !cancelAuditButton.isDisabled(); }
    double auditProgress() { return auditProgressBar.getProgress(); }
    String auditStatusText() { return auditStatusLabel.getText(); }
    String auditResultText() { return auditResultLabel.getText(); }
    int auditRunCount() { return auditRunCount; }
    void startAuditForEvidence() { startAuditButton.fire(); }
    void cancelAuditForEvidence() { cancelAuditButton.fire(); }
    void useControlledAuditFailure(boolean value) { controlledFailureCheck.setSelected(value); }

    void enterPlan(String name, String date, NutritionPlan.Status status) {
        nameField.setText(name);
        effectiveFromField.setText(date);
        statusBox.setValue(status);
    }

    void setDateInput(String date) { effectiveFromField.setText(date); }
    void submitPlan() { addButton.fire(); }
    void activateSelection() { activateButton.fire(); }
    void selectPlanById(long id) {
        for (var index = 0; index < visiblePlans.size(); index++) {
            if (visiblePlans.get(index).id() == id) {
                planTable.getSelectionModel().select(index);
                return;
            }
        }
        throw new IllegalArgumentException("В таблице нет плана с ID " + id);
    }
    void sortByNameDescending() {
        nameColumn.setSortType(TableColumn.SortType.DESCENDING);
        planTable.getSortOrder().clear();
        planTable.getSortOrder().add(nameColumn);
        planTable.sort();
    }
    long selectedPlanId() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        return selected == null ? 0 : selected.id();
    }
    NutritionPlan.Status selectedPlanStatus() {
        var selected = planTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.status();
    }
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
