# Соответствие полей FXML и контроллера

| Поле пользователя | Узел FXML | `fx:id` | Поле контроллера | Участие в сохранении |
|---|---|---|---|---|
| Название плана | `TextField` | `nameField` | `TextField nameField` | `nameField.getText()` передаётся в `NutritionPlanService.addPlan` |
| Дата начала | `DatePicker` | `effectiveFromPicker` | `DatePicker effectiveFromPicker` | `effectiveFromPicker.getValue()` передаётся в сервис |
| Состояние | `ComboBox<NutritionPlan.Status>` | `statusBox` | `ComboBox<NutritionPlan.Status> statusBox` | `statusBox.getValue()` передаётся в сервис |

Кнопка `addButton` связана с обработчиком `#addPlan`. `FXMLLoader` сначала внедряет все поля, затем вызывает `initialize`, где контроллер явно проверяет каждую обязательную связь. Поэтому опечатка `nameField → brokenNameField` обнаруживается при загрузке, а не маскируется до случайного клика пользователя.

Подписи `nameLabel`, `dateLabel` и `statusLabel` получают `labelFor` для соответствующих элементов. В FXML поля расположены в порядке: название, дата, состояние, сохранение. Этот же порядок образует клавиатурный маршрут формы.
