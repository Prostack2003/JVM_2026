# Матрица проверок практики 13

| Сценарий | Ожидаемый результ | Доказательство |
|---|---|---|
| полный аудит | 6 проверок на план, точная сводка | `publishesResultOnlyAfterEveryCheckCompletes`, `audit-succeeded.png` |
| изменение исходного списка | фон видит прежний снимок | `inputIsCopiedBeforeLongWorkStarts` |
| отмена после двух шагов | цикл остановлен, `AuditResult` нет | `cancellationStopsAtBoundaryAndReturnsNoPartialResult`, `audit-cancelled.png` |
| прерывание ожидания | `InterruptedException` не скрыт | `interruptionFromBlockingStepIsNotHidden` |
| повтор ID | `FAILED`, причина сохранена | `duplicateStableIdFailsInsteadOfPublishingSummary` |
| контролируемый UI-отказ | кнопки восстановлены, причина видна, прежний итог сохранён | `captureAuditEvidence`, `audit-failed.png` |
| окно во время фона | FX-цикл обрабатывает события | счётчик в `captureAuditEvidence`, `audit-running.png` |
| `Task.call` трогает UI | таких импортов/вызовов нет | `taskUsesObservableBridgeWithoutTouchingControls` |
| двойной старт | кнопка старта недоступна | `captureAuditEvidence`, `audit-running.png` |
| повтор после отмены/ошибки | новая `Task`, четвёртый запуск успешен | `audit-repeated.png` |
| регрессия 1–12 | прежние модель, CRUD, proxy, SQLite, миграции и импорт работают | `clean check`, `captureEvidence`, smoke |
| автономный повтор | сеть не нужна | команды с `--offline` |
