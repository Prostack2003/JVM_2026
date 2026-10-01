# Практика 10. Аннотации, рефлексия и наблюдение за вызовами

Самостоятельный Gradle-проект дневника питания КБЖУ. Русские названия выбранных публичных операций задаются runtime-аннотацией, безопасно читаются через reflection, а динамический proxy считает обращения к прикладному интерфейсу, не меняя результаты и исключения сервиса.

## Требования

- JDK 25 (проверено на 25.0.4);
- Linux, macOS или Windows с графической средой;
- Интернет только для первой загрузки Gradle 9.1 и JavaFX 25; затем команды работают из кэша.

## Сборка и запуск

Из каталога `Практика_10`:

```shell
./gradlew clean check
./gradlew runLessonDemo
./gradlew run
```

Windows:

```text
gradlew.bat clean check
gradlew.bat runLessonDemo
gradlew.bat run
```

Точка входа приложения — `ru.npi.kbju.Launcher`. Законченный консольный показ темы практики — `ru.npi.kbju.lesson.ReflectionDemo`.

Короткий фактический запуск JavaFX с автоматическим закрытием:

```shell
./gradlew run --args="--smoke"
```

Только проверки аннотаций и proxy:

```shell
./gradlew testObservation
```

Проверочный JavaFX-сценарий со снимками:

```shell
./gradlew captureEvidence
```

После подготовки кэша автономная проверка:

```shell
./gradlew --offline clean check testObservation runLessonDemo
./gradlew --offline run --args="--smoke"
```

## Как устроено наблюдение

1. `NutritionPlanOperations` — публичный интерфейс для UI и proxy.
2. `@OperationTitle` имеет `RetentionPolicy.RUNTIME` и применяется только к методам. Ею отмечены `addPlan` и `changeStatus`.
3. `OperationCatalog` читает только public-методы интерфейса и выводит русские описания. Метод `all` без аннотации пропускается.
4. `ObservedNutritionPlanService` реализуется стандартным `Proxy`: отмеченные предметные вызовы учитываются, затем делегируются настоящему `NutritionPlanService`.
5. `equals`, `hashCode` и `toString` обработаны отдельно и не меняют метрики. `InvocationTargetException` разворачивается, поэтому вызывающий код получает исходную `PlanValidationException` или `DuplicatePlanException`.

Reflection не выбирает метод из пользовательской строки, не вызывает `setAccessible` и не читает закрытые поля. Аргументы операций не журналируются. Аннотация описывает операцию, но не заменяет проверки модели и сервиса.

## Ручная приёмка

В выводе `runLessonDemo` должны быть подтверждены четыре пункта:

- для `addPlan` и `changeStatus` найдены русские описания;
- `all` без аннотации пропущен;
- два отмеченных вызова дают счётчик `2`, а технические методы его не меняют;
- некорректное название приводит именно к `PlanValidationException`, без оболочки reflection.

## Документы и доказательства

- [Контракт и правила наблюдения](docs/operation-contract.md)
- [Карта ответственности](docs/responsibility-map.md)
- [Матрица проверок](docs/test-matrix.md)
- [C4: компоненты](docs/architecture/C4_компоненты.md)
- [ADR 010](docs/architecture/ADR_010_наблюдение_через_proxy.md)
- [Протокол проверки](docs/check-protocol.md)
- [Источники](docs/sources.md)

`docs/evidence/*.txt` и `*.png` — результаты фактических прогонов, а не материалы проекта преподавателя.
