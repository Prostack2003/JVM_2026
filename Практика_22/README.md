# Практика 22. Поставка приложения через `jpackage`

Самостоятельный Gradle-проект дневника питания КБЖУ. Практика создаёт app-image для текущей ОС, включает в него launcher, приложение, зависимости и сокращённую среду Java, а затем проверяет поставку фактическим запуском вне исходного проекта и IDE.

Проверенная поставка относится к Linux/amd64. Образы Windows и macOS необходимо собирать и проверять на соответствующих операционных системах.

## Требования

- JDK 25 с утилитами `jpackage` и `jdeps` (проверено на 25.0.4.1);
- Gradle Wrapper 9.1;
- Linux с GTK 3, Pango/Cairo, X11 или Wayland, Fontconfig/FreeType и OpenGL для проверенного графического запуска;
- графический сеанс для `verifyPackagedLaunch` и обычного запуска;
- Интернет нужен только при первоначальном заполнении кэша Gradle.

Проверка среды:

```shell
java -version
javac -version
jpackage --version
./gradlew --version
```

## Создание и приёмка app-image

Из каталога `Практика_22`:

```shell
./gradlew --offline clean check verifyDistribution
```

Команда выполняет полный набор тестов и последовательно:

1. собирает обычную структуру `installDist`;
2. печатает найденные `jdeps` зависимости;
3. создаёт `build/package/KbjuDiary` через `jpackage --type app-image`;
4. проверяет launcher, встроенный runtime, основной JAR, FXML, CSS и service descriptor;
5. убеждается, что внутрь образа не попали `.db` и `.log`;
6. копирует образ в отдельный временный каталог без исходников;
7. запускает скопированный launcher дважды: добавляет запись через форму и после перезапуска находит её в SQLite;
8. создаёт архив `build/distributions/KbjuDiary-22.0.0-linux-amd64.tar.gz` и файл SHA-256.

`verifyAppImage` проверяет структуру, но не заменяет фактический запуск. Приёмочным является `verifyPackagedLaunch`, входящий в `verifyDistribution`.

## Запуск готового образа без IDE

После сборки:

```shell
./build/package/KbjuDiary/bin/KbjuDiary
```

Либо распакуйте архив в любой отдельный каталог и запустите `KbjuDiary/bin/KbjuDiary`. Системный JDK для запуска не требуется: launcher использует Java из `KbjuDiary/lib/runtime`.

По умолчанию изменяемые данные находятся вне каталога установки:

```text
~/.kbju-diary/data/nutrition-plans.db
~/.kbju-diary/logs/
~/.kbju-diary/backup/
```

Для воспроизводимой проверки можно выбрать другой корень:

```shell
./build/package/KbjuDiary/bin/KbjuDiary --data-dir=/tmp/kbju-demo-data
```

Параметр с пустым значением или повторное указание `--data-dir` отклоняется до запуска JavaFX.

## Ресурсы и зависимости

FXML, CSS, SQL-миграции и service descriptor остаются classpath-ресурсами основного JAR. `jdeps` используется как отправная точка, но динамические зависимости JavaFX/FXML проверяются реальным запуском; поэтому список runtime-модулей дополнен `java.scripting` и другими модулями, необходимыми приложению во время выполнения.

Текущая поставка использует немодульный classpath внутри launcher, как в учебном маршруте. JavaFX может вывести предупреждение об unnamed module; оно не мешает загрузке окна, FXML/CSS и сохранению данных и зафиксировано как известное ограничение.

## Обновление

App-image содержит только исполняемые файлы и встроенную среду. Обновление выполняется заменой каталога `KbjuDiary` при закрытом приложении. Каталог `~/.kbju-diary` не удаляется и не перезаписывается; миграции схемы применяются приложением при следующем запуске. Перед обновлением рекомендуется создать резервную копию средствами практики 20.

## Разработка и регрессия

```shell
./gradlew --offline clean test
./gradlew --offline run --args="--smoke"
./gradlew run
```

`runLessonDemo` является псевдонимом полного сценария `verifyDistribution`.

## Документы

- [Манифест поставки](docs/delivery-manifest.md)
- [Протокол чистого запуска](docs/clean-launch-protocol.md)
- [Системные требования](docs/system-requirements.md)
- [Политика обновления](docs/update-policy.md)
- [Матрица проверок](docs/test-matrix.md)
- [Портфель проверок](docs/test-portfolio.md)
- [Карта ответственности](docs/responsibility-map.md)
- [ADR 022](docs/architecture/ADR_022_app_image_и_каталог_данных.md)
- [C4: развёртывание](docs/architecture/C4_развёртывание.md)
- [Протокол приёмки](docs/check-protocol.md)
- [Изученные источники](docs/sources.md)

Проект преподавателя использован как источник критериев и механизма `jpackage`. Его предметная область оборудования, имена приложения и код пакета `servicejournal` не переносились.
