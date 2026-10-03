# Практические работы по разработке приложений на JVM

Репозиторий содержит практические работы курса 2026 года. Все задания выполняются в предметной области дневника питания КБЖУ.

Каждая практика находится в отдельной папке и представляет собой самостоятельный проект: её можно собрать и запустить независимо от остальных.

## Практические работы

| Практика | Тема | Результат | Статус |
|---|---|---|---|
| [Практика 01](Практика_01/) | Выбор проекта и первый запуск Java | Консольная программа выводит название проекта, роль пользователя и решаемую проблему | Выполнена |
| [Практика 02](Практика_02/) | Данные, условия, циклы и методы | Расчёт количества и процента просроченных пересмотров целей КБЖУ с проверкой границ | Выполнена |
| [Практика 03](Практика_03/) | Предметная модель и допустимые состояния | Неизменяемый план питания, инварианты, состояния и JUnit-проверки | Выполнена |
| [Практика 04](Практика_04/) | Коллекции и типобезопасный счётчик обращений | Универсальный счётчик повторов, безопасное объединение и неизменяемый снимок | Выполнена |
| [Практика 05](Практика_05/) | Первое окно JavaFX и обработка событий | Таблица планов питания, форма добавления и контролируемый отказ на пустом вводе | Выполнена |
| [Практика 06](Практика_06/) | Наблюдаемые данные, таблица и привязки | Единый `ObservableList`, связанный счётчик, удаление и замена состояния записи | Выполнена |
| [Практика 07](Практика_07/) | Разметка FXML и оформление формы | FXML, CSS, контроллер с внедрённым сервисом и classpath-загрузка ресурсов | Выполнена |
| [Практика 08](Практика_08/) | Проверка ввода, исключения и доступность | Полевые ошибки без потери черновика, конкретные исключения и полный клавиатурный маршрут | Выполнена |
| [Практика 09](Практика_09/) | Слои приложения и замена хранилища | Прикладной сервис, порт репозитория, адаптер памяти и проверяемое направление зависимостей | Выполнена |
| [Практика 10](Практика_10/) | Аннотации, рефлексия и наблюдение за вызовами | Runtime-метаданные public-операций и динамический proxy с прозрачной передачей ошибок | Выполнена |
| [Практика 11](Практика_11/) | SQLite и параметризованные запросы JDBC | Постоянное локальное хранилище за прежним портом, ограничения схемы и безопасные запросы | Выполнена |
| [Практика 12](Практика_12/) | Транзакции, миграции и импорт | Атомарный CSV-импорт, полный rollback и однократные V001–V002 с сохранением старых данных | Выполнена |
| [Практика 13](Практика_13/) | Фоновая задача и отмена операции | JavaFX Task, прогресс, безопасная отмена, отказ и повтор без зависания окна | Выполнена |
| [Практика 14](Практика_14/) | Исполнители, виртуальные потоки и гонки данных | Детерминированная гонка, `AtomicInteger`, ожидание `Future`, отмена и закрытие виртуальных потоков | Выполнена |
| [Практика 15](Практика_15/) | HTTP и JSON на локальном сервере | Фоновый HTTP-клиент, валидация JSON, атомарная замена справочника и воспроизводимые отказы | Выполнена |
| [Практика 16](Практика_16/) | Кэш, повторный запрос и автономный режим | Атомарный JSON-кэш, ограниченный retry, TTL и честное отображение источника данных | Выполнена |
| [Практика 17](Практика_17/) | JUnit: правила предметной области | Параметризованные тесты инвариантов и границ `NutritionPlan` с подтверждённой временной мутацией | Выполнена |
| [Практика 18](Практика_18/) | Интеграционные и интерфейсные проверки | Временная SQLite, classpath-проверка FXML и короткий JavaFX-маршрут через реальные элементы формы | Выполнена |
| [Практика 19](Практика_19/) | Настройки, журналирование и диагностика | Внешняя конфигурация путей, ранняя проверка настроек и диагностические события без секретов | Выполнена |
| [Практика 20](Практика_20/) | Резервная копия и безопасное восстановление | Согласованный SQLite-снимок, контроль целостности и восстановление только в новый тестовый файл | Выполнена |
| [Практика 21](Практика_21/) | Сборка, JAR и загрузка расширений | Проверенный состав JAR, два формата отчёта через `ServiceLoader` и явный отказ без descriptor | Выполнена |

Остальные практики будут добавляться последовательно после проверки предыдущей.

## Отчёты

Оформленные отчёты по практическим работам 1–12 доступны в форматах DOCX и PDF в каталоге [Отчёты](Отчёты/). В [оглавлении отчётов](Отчёты/README.md) приведены прямые ссылки на все 24 файла.

## Требования

- JDK 25;
- Git;
- доступ к Интернету при первом запуске Gradle Wrapper конкретной практики.

## Запуск практики

```shell
cd Практика_01
./gradlew clean run
```

Для практики 02:

```shell
cd Практика_02
./gradlew clean check run
```

Для практики 03:

```shell
cd Практика_03
./gradlew clean test run
```

Для практики 04:

```shell
cd Практика_04
./gradlew clean check run
```

Для практики 05:

```shell
cd Практика_05
./gradlew clean check
./gradlew run
```

Для практики 06:

```shell
cd Практика_06
./gradlew clean check
./gradlew run
```

Для практики 07:

```shell
cd Практика_07
./gradlew clean check
./gradlew run
```

Для практики 08:

```shell
cd Практика_08
./gradlew clean check
./gradlew run
```

Для практики 09:

```shell
cd Практика_09
./gradlew clean check testService
./gradlew run
```

Для практики 10:

```shell
cd Практика_10
./gradlew clean check testObservation runLessonDemo
./gradlew run
```

Для практики 11:

```shell
cd Практика_11
./gradlew clean check testDatabase runLessonDemo
./gradlew run
```

Для практики 12:

```shell
cd Практика_12
./gradlew clean check testTransactions runLessonDemo
./gradlew run
```

Для практики 13:

```shell
cd Практика_13
./gradlew clean check testBackground
./gradlew run
```

Для практики 14:

```shell
cd Практика_14
./gradlew clean check testConcurrency runLessonDemo
./gradlew run
```

Для практики 15:

```shell
cd Практика_15
./gradlew clean check testHttpJson runLessonDemo
./gradlew run
```

Для практики 16:

```shell
cd Практика_16
./gradlew clean check testOfflineCatalog runLessonDemo
./gradlew run
```

Для практики 17:

```shell
cd Практика_17
./gradlew clean check testDomainRules
./gradlew run
```

Для практики 18:

```shell
cd Практика_18
./gradlew clean check
./gradlew clean testIntegrationAndUi
./gradlew run
```

Для практики 19:

```shell
cd Практика_19
./gradlew clean check testDiagnostics runLessonDemo
./gradlew run
```

Для практики 20:

```shell
cd Практика_20
./gradlew clean check testBackupRecovery runLessonDemo
./gradlew run
```

Для практики 21:

```shell
cd Практика_21
./gradlew clean check testExtensions runLessonDemo
./gradlew run
```

Для Windows:

```text
cd Практика_01
gradlew.bat clean run

cd Практика_02
gradlew.bat clean check run

cd Практика_03
gradlew.bat clean test run

cd Практика_04
gradlew.bat clean check run

cd Практика_05
gradlew.bat clean check
gradlew.bat run

cd Практика_06
gradlew.bat clean check
gradlew.bat run

cd Практика_07
gradlew.bat clean check
gradlew.bat run

cd Практика_08
gradlew.bat clean check
gradlew.bat run

cd Практика_09
gradlew.bat clean check testService
gradlew.bat run

cd Практика_10
gradlew.bat clean check testObservation runLessonDemo
gradlew.bat run

cd Практика_11
gradlew.bat clean check testDatabase runLessonDemo
gradlew.bat run

cd Практика_12
gradlew.bat clean check testTransactions runLessonDemo
gradlew.bat run

cd Практика_13
gradlew.bat clean check testBackground
gradlew.bat run

cd Практика_14
gradlew.bat clean check testConcurrency runLessonDemo
gradlew.bat run

cd Практика_15
gradlew.bat clean check testHttpJson runLessonDemo
gradlew.bat run

cd Практика_16
gradlew.bat clean check testOfflineCatalog runLessonDemo
gradlew.bat run

cd Практика_17
gradlew.bat clean check testDomainRules
gradlew.bat run

cd Практика_18
gradlew.bat clean check
gradlew.bat clean testIntegrationAndUi
gradlew.bat run

cd Практика_19
gradlew.bat clean check testDiagnostics runLessonDemo
gradlew.bat run

cd Практика_20
gradlew.bat clean check testBackupRecovery runLessonDemo
gradlew.bat run

cd Практика_21
gradlew.bat clean check testExtensions runLessonDemo
gradlew.bat run
```

Подробные требования, сценарии отказа и доказательства проверки находятся в README соответствующей практики.

## Структура репозитория

```text
JVM_2026_практики/
├── README.md
├── Практика_01/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_02/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_03/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_04/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_05/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_06/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_07/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_08/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_09/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_10/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_11/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_12/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_13/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_14/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_15/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_16/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_17/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_18/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_19/
│   ├── config/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_20/
│   ├── config/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── Практика_21/
│   ├── config/
│   ├── docs/
│   ├── gradle/
│   ├── src/
│   ├── build.gradle
│   └── README.md
└── Практика_22/ ... Практика_24/
```

Каталоги сборки, кэш Gradle и файлы IDE в репозиторий не добавляются.
