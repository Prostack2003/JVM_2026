# Манифест поставки

## Проверенный артефакт

| Поле | Значение |
|---|---|
| имя приложения | `KbjuDiary` |
| версия | `22.0.0` |
| платформа | Linux/amd64 |
| тип | `jpackage --type app-image` |
| launcher | `KbjuDiary/bin/KbjuDiary` |
| приложение | `KbjuDiary/lib/app/` |
| встроенная Java | `KbjuDiary/lib/runtime/` |
| архив | `build/distributions/KbjuDiary-22.0.0-linux-amd64.tar.gz` |
| контрольная сумма | одноимённый файл с суффиксом `.sha256` |

В `lib/app` входят основной JAR, JavaFX 25, SQLite JDBC, Jackson и SLF4J NOP. Основной JAR содержит FXML, CSS, SQL-миграции, учебные CSV и service descriptor форматов отчёта.

## Модули встроенной Java

Образ создаётся с явным набором:

```text
java.base, java.desktop, java.logging, java.management, java.naming,
java.net.http, java.scripting, java.sql, java.xml,
jdk.crypto.ec, jdk.httpserver, jdk.unsupported
```

`jdeps` помогает получить начальный список. `java.scripting` и динамические требования подтверждены фактическим JavaFX-запуском; приёмка не полагается только на статический анализ.

## Что не входит

- исходный код, Gradle и IDE;
- системный JDK;
- пользовательская SQLite-база;
- журналы и резервные копии;
- образы Windows или macOS.

Каталог `build` не хранится в Git. Проверенный архив воспроизводится задачей `verifyDistribution` из версионируемых исходников.
