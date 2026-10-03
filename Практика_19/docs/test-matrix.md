# Матрица проверок практики 19

| Вход или действие | Ожидаемый результат | Доказательство |
|---|---|---|
| штатный `config/application.properties` | пути определены, каталоги доступны | `runLessonDemo` |
| отдельный учебный root и пример конфигурации | база, журнал и копии перемещены под новый root | запуск с `-PkbjuDataDir` и `-PkbjuConfig` |
| файла конфигурации нет | документированные значения по умолчанию | `missingFileUsesDocumentedDefaultsUnderExplicitRoot` |
| отсутствуют `log.directory` и `backup.directory` | значения по умолчанию только для них | `missingOptionalKeysUseDocumentedDefaults` |
| `database.path` пуст | ранний отказ с ключом, без исходного значения | `blankValueNamesExactConfigurationKey` |
| путь содержит недопустимый символ | ранний диагностируемый отказ | `invalidPathIsRejectedBeforeApplicationWorkStarts` |
| на месте каталога журнала находится файл | подготовка отклонена с ключом `log.directory` | `fileInPlaceOfLogDirectoryFailsDuringPreparation` |
| причина содержит токен и личное название | журнал содержит событие, но не чувствительные строки | `ApplicationLogTest` |
| журнал открыт повторно после закрытия | обе сессии записаны | `closingLogReleasesHandlerAndAllowsNextSession` |
| полная регрессия | все прежние уровни зелёные | `./gradlew clean check` |
| автономный запуск | зависимости берутся из кэша | `./gradlew --offline clean check` |
