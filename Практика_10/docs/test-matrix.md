# Матрица проверок практики 10

| Сценарий | Ожидаемый результат | Автоматическое доказательство |
|---|---|---|
| Метод с `@OperationTitle` | найдено русское описание | `OperationCatalogTest.findsOnlyAnnotatedPublicOperationsInStableOrder` |
| `all` без аннотации | отсутствует в каталоге и счётчике | тесты каталога и proxy |
| Retention/Target | `RUNTIME`, только `METHOD` | `annotationIsRetainedAtRuntimeAndTargetsOnlyMethods` |
| Два предметных вызова | общий счётчик равен 2 | `proxyKeepsResultsAndCountsOnlyAnnotatedCommands` и `runLessonDemo` |
| `equals/hashCode/toString` | явная семантика, счётчик не меняется | `objectMethodsAreExplicitAndDoNotAffectMetrics` |
| Ошибка инварианта | точный тип `PlanValidationException` | `uncheckedDomainFailureIsNotHiddenByReflectionWrapper` |
| Проверяемый дубликат | точный тип `DuplicatePlanException` | `checkedDuplicateFailureKeepsItsExactTypeAndInstance` |
| Прямой вызов и proxy | одинаковый результат | тест proxy и `runLessonDemo` |
| Архитектурная граница | UI знает интерфейс, но не реализацию/адаптер | `ArchitectureBoundaryTest` |
| Регрессия JavaFX | FXML, CSS, форма, клавиатура и стабильный ID работают | `captureEvidence` и smoke-запуск |
