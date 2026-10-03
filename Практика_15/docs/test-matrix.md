# Матрица проверок практики 15

| Сценарий | Ожидаемый результат | Доказательство |
|---|---|---|
| HTTP 200 + корректный JSON | приняты 3 `FoodProduct` | `http200ReturnsFullyMappedCatalog`, `runLessonDemo` |
| фоновое выполнение | порт вызван из виртуального, а не JavaFX-потока | `refreshRunsInVirtualThreadAndPublishesOnlyImmutableCompleteSnapshot` |
| HTTP 500 | `HTTP_STATUS`, тело не парсится, снимок прежний | `http500IsRejectedBeforeErrorBodyCanBeParsedAsData`, demo |
| HTTP 200 + повреждённый JSON | `MALFORMED_JSON`, снимок прежний | `successfulStatusWithBrokenJsonHasSeparateFailureKind`, demo |
| HTTP 200 + отрицательная калорийность | `CONTRACT`, весь кандидат отклонён | `validJsonWithImpossibleNutritionValueFailsContract`, demo |
| неверный/отсутствующий тип поля | `CONTRACT` до публикации | `rejectsMissingFieldWrongTypeAndDomainViolation` |
| дубликат `code` | отклонён весь набор | `rejectsDuplicateCodesBeforePublishingList`, service test |
| неизвестное поле | принято для совместимости | `mapsCompleteContractAfterCheckingEveryField` |
| задержка дольше таймаута | `TIMEOUT` | `requestTimeoutIsReportedSeparately` |
| `close()` сервера | тот же порт доступен новому серверу | `closingServerReleasesExactPortForNextRun`, demo |
| направление зависимостей | integration не знает UI/infrastructure | `ArchitectureBoundaryTest` |
| регрессия 1–14 | прежние тесты, JavaFX-ресурсы и SQLite работают | `clean check`, `run --args="--smoke"` |
| автономный повтор | внешняя сеть не нужна | команда с `--offline` |
