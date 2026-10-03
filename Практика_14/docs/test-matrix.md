# Матрица проверок практики 14

| Сценарий | Ожидаемый результат | Доказательство |
|---|---|---|
| принудительная гонка | два increment дают `unsafe=1` | `DeterministicCounterRaceTest`, 20 повторов |
| атомарный счётчик | те же два задания дают `atomic=2` | `DeterministicCounterRaceTest`, `runLessonDemo` |
| несколько диагностик | все `Future` завершены, итоги в порядке входа | `awaitsEveryFutureAndPreservesSnapshotOrder` |
| фактическая параллельность | на барьере одновременно не менее двух задач | `peakConcurrency >= 2` |
| пустой снимок | 0 задач, пустой итог, executor закрыт | `emptySnapshotIsACompletedBatchAndStillClosesExecutor` |
| `null` вместо снимка/плана | отказ до запуска задач | `rejectsNullBeforeStartingAnyTask` |
| ошибка в одном `Future` | остальные отменены, исходная ошибка сохранена | `keepsOriginalFailureAsCauseAndCancelsRemainingWork` |
| отмена ожидающей задачи | `cancelled=true`, interrupt получен и флаг восстановлен | `WaitingTaskCancellationTest`, 10 повторов |
| закрытие | активных задач 0, все executor завершены | поля `activeTasksAfterClose`, `executorTerminated` |
| граница JDBC | задачи получают только immutable snapshot | API пакета `application.concurrency`, ADR 014 |
| регрессия 1–13 | прежние 53 теста, JavaFX-ресурсы и SQLite работают | `clean check`, `run --args="--smoke"` |
| автономный повтор | сеть не нужна | команда с `--offline` |
