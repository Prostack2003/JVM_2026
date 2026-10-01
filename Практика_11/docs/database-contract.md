# Контракт базы данных

## Отображение модели

| Поле `NutritionPlan` | Столбец SQLite | Ограничение/преобразование |
|---|---|---|
| `id` | `id INTEGER` | `PRIMARY KEY AUTOINCREMENT`; после вставки читается generated key |
| `name` | `name TEXT` | `NOT NULL`, длина обрезанного значения от 1 до 60 |
| — | `name_key TEXT` | `UNIQUE`; `name.strip().toLowerCase(Locale.ROOT)` |
| `effectiveFrom` | `effective_from TEXT` | ISO-8601 `yyyy-MM-dd`, `NOT NULL`, проверка формы даты |
| `status` | `status TEXT` | имя enum и `CHECK` по четырём допустимым значениям |

`name_key` является инфраструктурным индексным полем и не попадает в предметную модель. Оно обеспечивает то же правило уникальности, которое ранее защищал адаптер памяти.

## SQL и ресурсы

Начальная схема находится в `src/main/resources/ru/npi/kbju/infrastructure/db/migration/V001__nutrition_plans.sql` и попадает в JAR. Запросы остаются константами программы, а `name`, `name_key`, дата, статус и ID передаются только через параметры `PreparedStatement`.

`InitialSchema` выполняет `CREATE TABLE IF NOT EXISTS`. Реестр применённых версий и изменение существующей схемы намеренно не реализованы: это тема практики 12.

## Ошибки и ресурсы

- нарушение `nutrition_plan.name_key` преобразуется в `DuplicatePlanException`;
- отсутствие ID при обновлении приводит к `NoSuchElementException`;
- несовместимая дата или неизвестное состояние в существующей строке дают диагностируемый `IllegalStateException`, а не значение по умолчанию;
- прочие ошибки JDBC сохраняются как причина `IllegalStateException` уровня хранилища;
- соединение принадлежит репозиторию, запросы и результаты закрываются `try-with-resources`, контекст приложения закрывает репозиторий.
