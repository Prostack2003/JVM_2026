# Матрица проверок практики 11

| Сценарий | Ожидаемый результат | Автоматическое доказательство |
|---|---|---|
| V001 упакована | SQL-ресурс найден в JAR | `verifyPackagedResources`, `DatabaseSchemaResourceTest` |
| Вставка и чтение | все поля совпадают, ID назначен БД | `insertAndReadPreserveEveryFieldIncludingApostrophe` |
| Апостроф в названии | `План О'Нила` читается целиком | тот же тест и `runLessonDemo` |
| Повторное открытие | запись остаётся в файле | `recordSurvivesClosingAndReopeningDatabase`, `runLessonDemo` |
| Нормализованный дубликат | точный `DuplicatePlanException`, строки не меняются | `normalizedDuplicateBecomesDomainFailureWithoutChangingRows` |
| Обновление и удаление | используются устойчивые ID | `updateAndDeleteUseStableIdentifier` |
| Неизвестный ID | диагностируемый отказ | `updateOfUnknownIdentifierIsDiagnosable` |
| Обход модели | `CHECK` не пропускает неизвестный статус | `databaseConstraintsProtectAlternateWritePath` |
| Повреждённая дата | ошибка чтения, без тихой подмены | `corruptedDateIsReportedInsteadOfBeingSilentlyReplaced` |
| Граница слоёв | сервис зависит от порта, UI не знает адаптер | `ArchitectureBoundaryTest` |
| Регрессия интерфейса | форма, ошибки, клавиатура и стабильный ID работают поверх SQLite | `captureEvidence`, smoke-запуск |
| Автономный повтор | сборка и запуск не обращаются в сеть | команды с `--offline` |
