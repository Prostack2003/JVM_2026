# Матрица проверок практики 12

| Сценарий | Ожидаемый результат | Автоматическое доказательство |
|---|---|---|
| корректный CSV | все строки и поля сохранены | `validFileIsFullyParsedAndNormalized`, `batchImportCommitsAllFieldsAndCodesTogether`, `runLessonDemo` |
| ошибка в поздней строке CSV | указан номер, репозиторий не вызван | `malformedDateReportsExactLine`, `invalidLaterRowPreventsAnyRepositoryCall` |
| неверный заголовок или кавычки | понятный отказ до БД | `wrongHeaderIsRejectedBeforeAnyRows`, `unsupportedQuotesAreRejectedExplicitly` |
| дубликат кода внутри пакета | полный rollback, указан `KBJU-601` | `duplicateCodeRollsBackWholePackageAndNamesProblemCode`, `runLessonDemo` |
| разные коды, одинаковое нормализованное имя | дополнительный предметный rollback | `duplicateNormalizedNameAlsoRollsBackRowsWithDifferentCodes` |
| новая база | V001 и V002 выполнены по порядку, версия 2 | `freshDatabaseAppliesEachMigrationExactlyOnce` |
| повтор мигратора | нет повторных шагов, в журнале 2 строки | тот же тест, `runLessonDemo` |
| база практики 11 | старая строка сохранена, `import_code IS NULL`, версия 2 | `practiceElevenDatabaseKeepsOldRowWhenMigratedToV002` |
| ресурсы в JAR | FXML, CSS, V001, V002 и оба CSV найдены | `verifyPackagedResources`, `DatabaseSchemaResourceTest` |
| регрессия CRUD и UI | прежние 44 теста и JavaFX-сценарий проходят | `check`, `captureEvidence`, smoke-запуск |
| автономный повтор | сборка, демо и UI не обращаются в сеть | команды с `--offline` |
