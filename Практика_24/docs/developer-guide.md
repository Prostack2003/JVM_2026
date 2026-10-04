# Инструкция разработчика

## Среда

- JDK 25;
- Gradle Wrapper 9.1;
- доступ к Maven Central нужен только для первичной загрузки зависимостей;
- после подготовки используется `--offline`.

## Сборка и проверки

```shell
./gradlew --offline clean check
./gradlew --offline run --args="--smoke"
./gradlew --offline releaseAcceptance
```

`check` запускает накопительный портфель модульных, интеграционных, UI и interop-проверок. `releaseAcceptance` дополнительно запускает отказные демонстрации, app-image вне исходного дерева и собирает финальный архив.

## Основные границы

- `domain` содержит неизменяемые модели и контракты;
- `application` координирует сценарии;
- `infrastructure` содержит SQLite, конфигурацию, журнал и backup;
- `integration` содержит HTTP/JSON и файловый кэш;
- `ui` содержит JavaFX, FXML и CSS;
- `interop` содержит узкую Java–Kotlin-границу;
- `report` расширяется через `ServiceLoader`.

Kotlin-компонент форматирует Java-record и не обращается к UI или SQLite. `module-info.java` сохраняет явную зависимость `kotlin.stdlib`.

## Файлы данных

При обычном запуске корень данных выбирает `AppConfig`. Для изолированной проверки используйте `--data-dir=<путь>`. Тесты и демонстрации создают временные каталоги и не открывают пользовательскую базу.

## Выпуск

```shell
./gradlew --offline verifyDistribution
./gradlew --offline verifyReleaseBundle
```

Первая команда создаёт архив app-image и SHA-256. Вторая добавляет исходники, документацию и доказательства. Не добавляйте `build`, `.gradle`, `.kotlin`, `.lesson-data`, базы и журналы в Git.

## Изменение требований

Перед новой функцией обновите `mini_tz.md` и `traceability-matrix.md`. Новая строка не получает статус `ПРОЙДЕНО`, пока команда и фактическое доказательство не существуют.
