# Карта ответственности

| Участник | Делает | Не делает |
|---|---|---|
| `NutritionPlan` | хранит состояние и защищает инварианты | не зависит от аннотаций и proxy |
| `NutritionPlanOperations` | задаёт открытый прикладной контракт и метаданные двух команд | не реализует бизнес-правила |
| `NutritionPlanService` | выполняет сценарии через порт репозитория | не считает вызовы и не знает JavaFX |
| `OperationCatalog` | читает аннотации public-методов | не вызывает методы и не открывает private-члены |
| `ObservedNutritionPlanService` | считает отмеченные вызовы и делегирует сервису | не меняет результат, причину отказа и аргументы |
| `OperationCallMetrics` | хранит обезличенные счётчики по имени метода | не журналирует входные данные |
| `MainController` | переводит форму в вызовы интерфейса | не знает реализацию сервиса или proxy |
| `ApplicationContext` | связывает репозиторий, сервис, метрики и proxy | не содержит предметных правил |

```text
MainController ──> NutritionPlanOperations <── implements ── dynamic proxy
                                                       │
                                                       └──> NutritionPlanService
                                                               │
                                                               └──> NutritionPlanRepository
ApplicationContext создаёт все конкретные объекты и отдаёт UI только интерфейс.
```
