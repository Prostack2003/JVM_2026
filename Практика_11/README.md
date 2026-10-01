# Практика 11. SQLite и параметризованные запросы JDBC

Самостоятельный Gradle-проект дневника питания КБЖУ. Приложение сохраняет планы питания в локальном файле SQLite через JDBC. Прикладной сервис и интерфейс `NutritionPlanRepository` остались прежними: память процесса заменена инфраструктурным адаптером без изменения UI и бизнес-сценариев.

## Требования

- JDK 25 (проверено на 25.0.4);
- Linux, macOS или Windows с графической средой;
- Интернет только при первой загрузке Gradle 9.1, JavaFX 25 и SQLite JDBC; затем проект проверяется из кэша.

## Сборка и запуск

Из каталога `Практика_11`:

```shell
./gradlew clean check testDatabase runLessonDemo
./gradlew run
```

Windows:

```text
gradlew.bat clean check testDatabase runLessonDemo
gradlew.bat run
```

Точка входа приложения — `ru.npi.kbju.Launcher`. Воспроизводимая консольная приёмка SQLite — `ru.npi.kbju.lesson.DatabaseDemo`.

Короткий запуск реального JavaFX-окна с автоматическим закрытием:

```shell
./gradlew run --args="--smoke"
```

Регрессионный UI-сценарий со снимками:

```shell
./gradlew captureEvidence
```

После первой загрузки зависимостей автономная проверка:

```shell
./gradlew --offline clean check testDatabase runLessonDemo
./gradlew --offline run --args="--smoke"
```

## Где лежат данные

При `./gradlew run` база создаётся в `Практика_11/.lesson-data/nutrition-plans.db`. Каталог исключён из Git. При обычном запуске вне Gradle используется `${user.home}/.kbju-diary/nutrition-plans.db`; каталог можно заменить системным свойством `kbju.data.dir`. Абсолютный путь в исходном коде не зашит, а фактический путь печатается только для диагностики.

Схема хранится как classpath-ресурс `src/main/resources/ru/npi/kbju/infrastructure/db/migration/V001__nutrition_plans.sql`. Это только начальная идемпотентная схема: механизм версий, транзакционные сценарии и импорт относятся к следующей практике.

## Контракт хранения

- `id` — первичный ключ SQLite и устойчивый идентификатор модели;
- `name`, `effective_from`, `status` сохраняются и восстанавливаются без подмены;
- `name_key` — технический нормализованный ключ для ограничения `UNIQUE`, поскольку у модели плана нет отдельного предметного кода;
- все пользовательские значения передаются через `PreparedStatement` и параметры `?`;
- `Connection`, `PreparedStatement` и `ResultSet` закрываются контролируемо;
- конфликт `UNIQUE` преобразуется в понятный `DuplicatePlanException`, прочие ошибки хранения не маскируются.

## Ручная приёмка

`./gradlew runLessonDemo` должен подтвердить:

1. после вставки прочитаны те же четыре поля;
2. название `План О'Нила` сохранено целиком;
3. после закрытия и повторного открытия файла запись существует;
4. нормализованный дубликат отклонён предметным исключением, а данные не изменились.

## Документы и доказательства

- [Контракт базы данных](docs/database-contract.md)
- [Карта ответственности](docs/responsibility-map.md)
- [Матрица проверок](docs/test-matrix.md)
- [ADR 011](docs/architecture/ADR_011_sqlite_адаптер.md)
- [C4: компоненты](docs/architecture/C4_компоненты.md)
- [Протокол проверки](docs/check-protocol.md)
- [Изученные источники](docs/sources.md)

`docs/evidence/*.txt` и `*.png` — результаты фактических прогонов этого проекта. Файлы и готовые предметные решения проекта преподавателя не копировались.
