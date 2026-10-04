# Практика 23. Java и Kotlin в одном проекте

Самостоятельный Gradle-проект дневника питания КБЖУ. В накопленное Java-приложение добавлен один изолированный Kotlin-компонент `NutritionPlanCaption`: Java вызывает его как обычный статический метод, а Kotlin читает существующий Java-record `NutritionPlan`. UI, SQLite, прикладные сервисы и предметные правила остались на Java.

## Требования

- JDK 25 (проверено на 25.0.4.1);
- Gradle Wrapper 9.1;
- Kotlin Gradle Plugin и stdlib 2.4.10;
- Интернет только для первой загрузки Kotlin Plugin и зависимостей;
- после подготовки полный сценарий выполняется с `--offline`.

Проверка среды:

```shell
java -version
javac -version
./gradlew --version
```

## Граница Java и Kotlin

| Сторона | Декларация | Наблюдаемый результат |
|---|---|---|
| Kotlin | `object NutritionPlanCaption` | один экземпляр форматировщика без состояния |
| Kotlin | `@JvmStatic fun format(plan: NutritionPlan?): String` | Java видит публичный статический метод `format(NutritionPlan)` |
| Java | `NutritionPlan` | Kotlin читает компоненты record и вложенный enum |
| Java | `InteropDemo` | вызывает Kotlin по имени `NutritionPlanCaption.format(...)` |

Nullable-параметр выбран намеренно. Java может передать `null`, поэтому Kotlin выполняет `requireNotNull` и выдаёт устойчивый отказ `IllegalArgumentException: План питания обязателен`. Оператор `!!` не используется.

## Основная проверка

Из каталога `Практика_23`:

```shell
./gradlew --offline clean check runLessonDemo
```

Ожидаемый вывод учебной демонстрации:

```text
План #23: Поддержание рациона [ACTIVE], действует с 2026-10-23
Null-контракт: План питания обязателен
```

`check` запускает полный накопительный набор и отдельно `KotlinInteropTest`. Проверяются:

- обычная запись и точная строка Kotlin-форматировщика;
- граничный идентификатор `0` и состояние `NEEDS_REVIEW`;
- явный отказ при Java-вызове с `null`;
- публичная статическая JVM-сигнатура;
- наличие Kotlin-класса и `kotlin-stdlib-2.4.10.jar` в `installDist`;
- запуск Java-клиента из собранного дистрибутива, а не из исходников.

## Проверка `installDist`

```shell
./gradlew --offline clean installDist verifyKotlinDistribution
```

Артефакты:

```text
build/install/jvm-practice-23/lib/jvm-practice-23-23.0.0.jar
build/install/jvm-practice-23/lib/kotlin-stdlib-2.4.10.jar
```

Задача `verifyKotlinDistribution` проверяет класс `ru/npi/kbju/interop/NutritionPlanCaption.class`, наличие runtime и запускает `InteropDemo` с classpath, составленным только из `installDist/lib`.

## JPMS

Проект сохраняет `module-info.java`. Kotlin компилируется первым, после чего `javac` получает его классы через `--patch-module` как часть того же модуля `ru.npi.kbju.desktop`. Зависимость `requires kotlin.stdlib` объявлена явно. Это настройка этапа компиляции, а не второй runtime-модуль приложения.

## Накопленная поставка

Накопленный app-image также собирается и теперь включает Kotlin-класс и stdlib:

```shell
./gradlew --offline verifyDistribution
```

Фактический JavaFX-запуск скопированного app-image по-прежнему проверяет FXML, CSS, внешний каталог данных и сохранение SQLite после перезапуска.

## Обычное приложение

```shell
./gradlew --offline run --args="--smoke"
./gradlew run
```

Добавление Kotlin-компонента не меняет пользовательский интерфейс и формат базы данных.

## Документы

- [Контракт взаимодействия](docs/interop-contract.md)
- [Изменение состава поставки](docs/distribution-delta.md)
- [Матрица проверок](docs/test-matrix.md)
- [Портфель проверок](docs/test-portfolio.md)
- [Карта ответственности](docs/responsibility-map.md)
- [ADR 023](docs/architecture/ADR_023_Kotlin_форматировщик.md)
- [C4: компоненты](docs/architecture/C4_компоненты.md)
- [Протокол приёмки](docs/check-protocol.md)
- [Изученные источники](docs/sources.md)

Проект преподавателя использован как источник механизма `@JvmStatic`, null-контракта и проверки `installDist`. Его `Equipment`, формат строк и пакет `servicejournal` не переносились.
