# Карта ответственности

| Участник | Делает | Не делает |
|---|---|---|
| `NutritionPlan` | хранит состояние и защищает инварианты | не знает JDBC, таблиц и путей |
| `NutritionPlanRepository` | задаёт прежний порт хранения | не навязывает конкретную БД |
| `NutritionPlanService` | выполняет прежние сценарии через порт | не знает, что память заменена SQLite |
| `SqliteNutritionPlanRepository` | отображает модель в строки, параметризует запросы, преобразует ошибки | не содержит UI- и прикладных правил |
| `InitialSchema` | читает V001 из classpath и создаёт таблицу | не выполняет миграции между версиями |
| `ApplicationDataPaths` | выбирает переносимый каталог и имя файла | не содержит абсолютных путей среды разработчика |
| `ApplicationContext` | связывает SQLite-адаптер, сервис, proxy и UI; закрывает ресурсы | не содержит SQL и бизнес-правил |
| `MainController` | переводит действия формы в вызовы сервиса | не открывает соединение и не выполняет SQL |

```text
MainController → NutritionPlanOperations → NutritionPlanService
                                             │
                                             ↓
                                  NutritionPlanRepository
                                             ↑
                               SqliteNutritionPlanRepository
                                     │                 │
                                     ↓                 ↓
                              PreparedStatement   файл SQLite
```
