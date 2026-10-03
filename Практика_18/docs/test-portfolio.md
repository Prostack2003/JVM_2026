# Портфель проверок практики 18

| Риск | Самая узкая достаточная граница | Проверка |
|---|---|---|
| недопустимый план создан | предметная модель | `NutritionPlanTest` |
| сервис обходит правило или неверно обновляет запись | application + memory adapter | `NutritionPlanServiceTest` |
| SQL, миграция, тип или ограничение работают неверно | реальная временная SQLite | `SqliteNutritionPlanRepositoryTest` |
| запись исчезает после закрытия соединения | закрытие и повторное открытие временного файла | `recordSurvivesClosingAndReopeningDatabase` |
| нарушены HTTP, JSON или автономный кэш | loopback HTTP + временные файлы | тесты пакетов `integration` и `application.catalog` |
| FXML отсутствует или потерял контроллер/обработчик | classpath-ресурс и собранный JAR | `FxmlResourceTest`, `verifyPackagedResources` |
| кнопка не передаёт введённые значения сервису | реальные FXML-контролы в FX Thread | `JavaFxUserJourneyTest` |
| ошибочный ввод всё же сохраняется | UI + memory adapter | `blankNameShowsFieldErrorAndDoesNotSave` |
| внешний вид или доступность отличаются на целевой ОС | ручной запуск | клавиатура, изменение размера, визуальный осмотр |

Один уровень не подменяет другой. Memory adapter делает пользовательский маршрут быстрым и изолированным, а отдельный SQLite-тест подтверждает настоящие SQL, миграции и повторное открытие файла.
