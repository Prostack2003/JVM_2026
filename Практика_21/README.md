# Практика 21. Сборка, JAR и загрузка расширений

Самостоятельный Gradle-проект дневника питания КБЖУ. Практика собирает обычный JAR, проверяет наличие классов и ресурсов, а затем запускает из этого JAR пример `ServiceLoader` с двумя поставщиками одного интерфейса. Поставщики создают полный и компактный суточные отчёты без изменения клиентского цикла.

## Требования

- JDK 25 (проверено на 25.0.4);
- Gradle Wrapper 9.1;
- Интернет нужен только при первоначальном заполнении кэша Gradle;
- графический сеанс нужен только для запуска накопленного JavaFX-приложения.

Версии среды:

```shell
java -version
javac -version
./gradlew --version
```

## Структура расширения

| Элемент | Назначение |
|---|---|
| `NutritionReportPlugin` | общий публичный контракт формата отчёта |
| `PlainNutritionReport` | полный человекочитаемый формат |
| `CompactNutritionReport` | короткий однострочный формат |
| `META-INF/services/ru.npi.kbju.report.NutritionReportPlugin` | classpath-регистрация двух поставщиков |
| `PluginDemo` | клиент, который знает только интерфейс и `ServiceLoader` |

Точка входа учебного сценария: `ru.npi.kbju.lesson.PluginDemo`. Оба поставщика имеют публичный конструктор без аргументов. В `module-info.java` тот же контракт объявлен через `uses` и `provides`, поэтому решение остаётся корректным и в именованном модуле.

## Сборка и основная проверка

Из каталога `Практика_21`:

```shell
./gradlew clean testExtensions verifyJarContents runLessonDemo
```

Windows:

```text
gradlew.bat clean testExtensions verifyJarContents runLessonDemo
```

`runLessonDemo` запускает `PluginDemo` именно из `build/libs/jvm-practice-21-21.0.0.jar`, а не из каталога скомпилированных классов. Ожидаются два различных отчёта и строка `Поставщиков: 2`.

## Проверка состава JAR

После сборки содержимое можно посмотреть стандартным инструментом JDK:

```shell
jar tf build/libs/jvm-practice-21-21.0.0.jar
```

В архиве должны находиться:

- `ru/npi/kbju/lesson/PluginDemo.class`;
- классы интерфейса и двух поставщиков;
- `ru/npi/kbju/ui/main-view.fxml`;
- `ru/npi/kbju/ui/style.css`;
- `META-INF/services/ru.npi.kbju.report.NutritionReportPlugin`.

Задача `verifyJarContents` проверяет этот белый список и точный порядок двух строк descriptor автоматически.

## Контролируемый отказ

```shell
./gradlew descriptorlessJar verifyMissingProviderFailure
```

`descriptorlessJar` создаёт отдельную учебную копию `*-without-services.jar`, не изменяя основной JAR и исходники. Проверка запускает копию на classpath, требует ненулевой код завершения и сообщение `Поставщики отчётов не найдены`. Наличие классов поставщиков без descriptor недостаточно для classpath-обнаружения.

## Полная регрессия и приложение

```shell
./gradlew clean check
./gradlew run --args="--smoke"
./gradlew run
```

`check` включает специализированные тесты, проверку JAR и отрицательный запуск. Накопленное JavaFX-приложение по-прежнему использует FXML, CSS и SQLite.

## Граница доверия

`ServiceLoader` обнаруживает и создаёт реализации, но не ограничивает их полномочия. Эта практика регистрирует только два поставщика из исходников проекта. Установка произвольных внешних JAR, загрузка из пользовательского каталога и самодельное преобразование байтов не считаются защитой и не реализованы.

## Документы

- [Контракт расширения](docs/extension-contract.md)
- [Состав JAR](docs/jar-inventory.md)
- [Матрица проверок](docs/test-matrix.md)
- [Портфель проверок](docs/test-portfolio.md)
- [Карта ответственности](docs/responsibility-map.md)
- [ADR 021](docs/architecture/ADR_021_ServiceLoader_для_отчётов.md)
- [C4: компоненты](docs/architecture/C4_компоненты.md)
- [Протокол приёмки](docs/check-protocol.md)
- [Изученные источники](docs/sources.md)

Проект преподавателя использован только как источник критериев, механизма `ServiceLoader` и сценария отказа. Его предметная область оборудования и код пакета `servicejournal` не переносились.
