# Манифест поставки

## `installDist`

| Поле | Значение |
|---|---|
| проект | `jvm-practice-23` |
| версия | `23.0.0` |
| основной JAR | `jvm-practice-23-23.0.0.jar` |
| Kotlin-компонент | `ru/npi/kbju/interop/NutritionPlanCaption.class` |
| Kotlin runtime | `kotlin-stdlib-2.4.10.jar` |
| Java-демонстрация | `ru.npi.kbju.interop.InteropDemo` |

`verifyKotlinDistribution` подтверждает эти элементы и запускает демонстрацию из `installDist/lib`.

## App-image

| Поле | Значение |
|---|---|
| имя | `KbjuDiary` |
| платформа проверки | Linux/amd64 |
| launcher | `KbjuDiary/bin/KbjuDiary` |
| встроенная Java | `KbjuDiary/lib/runtime/` |
| приложение и зависимости | `KbjuDiary/lib/app/` |
| архив | `build/distributions/KbjuDiary-23.0.0-linux-amd64.tar.gz` |

App-image должен содержать основной JAR, Kotlin stdlib, JavaFX, SQLite JDBC, Jackson, FXML, CSS и service descriptor. Пользовательские `.db` и `.log` в образ не входят.

Каталог `build` не хранится в Git. Оба типа поставки воспроизводятся из версионируемых исходников задачами `installDist` и `verifyDistribution`.
