# Портфель проверок практики 19

| Риск | Граница | Проверка |
|---|---|---|
| относительный путь зависит от каталога терминала | `AppConfig` с явным root | `relativeValuesResolveFromDataRootAndDirectoriesArePrepared` |
| отсутствие необязательного значения ломает запуск | загрузка Properties | проверки файла и ключа по умолчанию |
| пустое или неверное значение обнаруживается поздно | ранняя конфигурация | `blankValueNamesExactConfigurationKey`, `invalidPathIsRejectedBeforeApplicationWorkStarts` |
| каталог нельзя создать | файловая граница во временном каталоге | `fileInPlaceOfLogDirectoryFailsDuringPreparation` |
| токен или личный план попадает в лог | настоящий `FileHandler` во временном каталоге | `failureContainsOperationReasonAndEventIdButNotSensitiveCause` |
| обработчик удерживает lock-файл | жизненный цикл журнала | `closingLogReleasesHandlerAndAllowsNextSession` |
| прежняя функциональность нарушена | полный портфель практики 18 | `clean check` |

Тесты не изменяют системные настройки пользователя: каждый файловый сценарий использует собственный `@TempDir`.
