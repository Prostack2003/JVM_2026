# Состав JAR практики 21

Основной артефакт: `build/libs/jvm-practice-21-21.0.0.jar`.

## Обязательный минимум

| Категория | Элементы |
|---|---|
| точка демонстрации | `ru/npi/kbju/lesson/PluginDemo.class` |
| SPI | интерфейс, DTO и два класса поставщиков |
| регистрация | `META-INF/services/ru.npi.kbju.report.NutritionReportPlugin` |
| интерфейс приложения | `ru/npi/kbju/ui/main-view.fxml`, `style.css` |
| данные и миграции | CSV-примеры и V001–V002 |
| модуль | `module-info.class` |

Проверка выполняется командами:

```shell
./gradlew clean jar verifyJarContents
jar tf build/libs/jvm-practice-21-21.0.0.jar
```

Основной JAR не является fat JAR: зависимости JavaFX, Jackson и SQLite остаются отдельными библиотеками времени выполнения. Для учебного `PluginDemo` хватает `java.base`, поэтому сценарий запускается непосредственно из JAR. Полное настольное приложение запускается через Gradle Application; подготовка app-image относится к практике 22.
