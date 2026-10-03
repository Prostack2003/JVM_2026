# Практика 15. HTTP и JSON на локальном сервере

Самостоятельный Gradle-проект дневника питания КБЖУ. Проект получает справочник продуктов по HTTP с локального loopback-сервера, проверяет статус и JSON-контракт и только после полной валидации заменяет подтверждённый каталог.

## Требования

- JDK 25 (проверено на 25.0.4);
- Linux, macOS или Windows;
- графическая среда только для запуска наследуемого JavaFX-окна;
- Интернет только при первой загрузке Gradle 9.1, JavaFX 25, Jackson 2.20.0 и SQLite JDBC.

## Сборка и приёмка

Из каталога `Практика_15`:

```shell
./gradlew clean check testHttpJson runLessonDemo
```

Windows:

```text
gradlew.bat clean check testHttpJson runLessonDemo
```

`runLessonDemo` — основной маршрут приёмки. Точка входа: `ru.npi.kbju.lesson.HttpJsonDemo`. Он проверяет:

1. `HTTP 200` и целый JSON превращаются в три `FoodProduct`;
2. `HTTP 500` даёт причину `HTTP_STATUS`, а прежний каталог не меняется;
3. повреждённый JSON даёт `MALFORMED_JSON` и не публикует частичный результат;
4. отрицательная калорийность даёт предметную причину `CONTRACT`;
5. после `close()` тот же порт занимается новым сервером.

Повтор без сети:

```shell
./gradlew --offline clean check testHttpJson runLessonDemo
```

## HTTP/JSON-граница

Клиент выполняет `GET /api/v1/products`, передаёт `Accept: application/json`, имеет таймауты соединения 2 с и запроса 3 с, а также предел ответа 64 КиБ. Статус и `Content-Type` проверяются до разбора тела.

`ProductCatalogRefreshService` запускает блокирующий клиент через переданный фоновый `Executor`; в JavaFX Application Thread запрос не выполняется. Порт `ProductDirectory` не знает о HTTP и Jackson. Подробности: [HTTP/JSON-контракт](docs/http-json-contract.md).

Кэш, retry и автономный режим в эту практику не входят.

## Наследуемое окно

Практика сохраняет функции практик 1–14:

```shell
./gradlew run
./gradlew run --args="--smoke"
```

Данные окна хранятся в `Практика_15/.lesson-data/nutrition-plans.db`; каталог исключён из Git.

## Документы

- [HTTP/JSON-контракт](docs/http-json-contract.md)
- [Пример корректного JSON](docs/examples/products-valid.json)
- [ADR 015](docs/architecture/ADR_015_http_json_boundary.md)
- [C4: компоненты](docs/architecture/C4_компоненты.md)
- [Карта ответственности](docs/responsibility-map.md)
- [Матрица проверок](docs/test-matrix.md)
- [Протокол проверки](docs/check-protocol.md)
- [Изученные источники](docs/sources.md)

Проект преподавателя использован как источник критериев. Его предметные классы, ответы и готовая демонстрация не копировались.
