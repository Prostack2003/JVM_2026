# Карта ответственности

| Участник | Делает | Не делает |
|---|---|---|
| `NutritionPlanImportRow` | нормализует код и повторяет инварианты плана | не знает CSV, JDBC и SQL |
| `NutritionPlanCsvParser` | читает и проверяет весь CSV | не открывает транзакцию |
| `NutritionPlanImportService` | после успешного разбора передаёт пакет порту | не знает SQLite |
| `NutritionPlanBatchRepository` | задаёт порт атомарной записи и чтения по коду | не навязывает технологию хранения |
| `SqliteNutritionPlanRepository` | выполняет пакет одним `Connection`, управляет `commit`/`rollback`, сохраняет причину ошибки | не разбирает файловый формат |
| `SchemaMigrator` | ведёт журнал, применяет V001 и V002 по порядку и однократно | не знает прикладные сценарии |
| `MainController` | сохраняет прежний CRUD-интерфейс | не выполняет импорт в фоне; это не тема этапа |

```text
CSV → NutritionPlanCsvParser → NutritionPlanImportService
                                      ↓
                           NutritionPlanBatchRepository
                                      ↑
                        SqliteNutritionPlanRepository
                              ↓              ↓
                         transaction       SQLite V002
                                               ↑
                                      SchemaMigrator
```
