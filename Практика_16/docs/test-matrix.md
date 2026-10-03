# Матрица проверок практики 16

| Сценарий | Ожидаемый результат | Доказательство |
|---|---|---|
| сервер доступен | `NETWORK`, 3 записи, кэш с тем же временем | integration test, `runLessonDemo` |
| HTTP 503/500 при свежем кэше | две попытки, `CACHE`, возраст и `HTTP_STATUS` | service test, integration test, demo |
| повреждённый ответ | `MALFORMED_JSON`, файл кэша побайтово не изменён | integration test, demo |
| первый offline-запуск | `NO_SAVED_DATA`, действие повтора | service test, demo |
| повреждённая копия кэша | `CORRUPT_CACHE`, записи не показаны | cache/service tests, demo |
| ровно 24 часа | кэш ещё допустим | `cacheIsAcceptedAtExactTtl...`, demo |
| 24 часа + 1 нс | `EXPIRED_CACHE` | `cacheIsAcceptedAtExactTtl...` |
| часы переведены назад | `CLOCK_ROLLBACK` | `cacheIsAcceptedAtExactTtl...` |
| HTTP 401 при наличии кэша | одна попытка, ошибка не скрыта | `authorizationFailureDoesNotRetryOrHideBehindCache` |
| два одновременных refresh | второй получает `UPDATE_IN_PROGRESS`; после завершения повтор работает | `secondParallelRefreshIsRejectedButManualRetryWorksAfterCompletion` |
| две последовательные записи кэша | остаётся только новый целый снимок без temp-файла | `FileProductCatalogCacheTest` |
| границы слоёв | application не знает о HTTP/JSON/Path, integration не знает UI | `ArchitectureBoundaryTest` |
| регрессия 1–15 | прежние тесты, JavaFX и SQLite работают | `clean check`, два smoke-запуска |
| автономная сборка | внешняя сеть не нужна | команда с `--offline` |
