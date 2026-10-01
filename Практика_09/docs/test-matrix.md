# Матрица проверок практики 09

| Сценарий | Ожидаемый результат | Автоматическое доказательство |
|---|---|---|
| Новое название | репозиторий назначает ID, запись появляется | `NutritionPlanServiceTest.createsPlanThroughRepositoryAndKeepsValues` |
| Повтор названия без учёта регистра | `DuplicatePlanException`, коллекция и последовательность ID не меняются | тесты сервиса и памяти `duplicateFailure...` |
| Второй репозиторий | независимое пустое состояние | `MemoryNutritionPlanRepositoryTest.repositoriesHaveIndependentState` |
| Изменение по ID | заменяется именно найденная запись | `changesAndRemovesPlanByStableIdentifier` |
| Сортировка таблицы | после обновления выбран тот же ID | `captureEvidence`, снимок `ui-stable-id.png` |
| Без окна | сервисные тесты проходят отдельно | `./gradlew testService` |
| Направление зависимостей | внутренние слои не импортируют JavaFX/UI/адаптер | `ArchitectureBoundaryTest` |

Отказ при дубликате проверяется до изменения коллекции и до увеличения `nextId`. Это исключает частично сохранённое состояние.
