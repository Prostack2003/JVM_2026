# Протокол проверки практики 23

Дата проверки: 4 октября 2026 года.

## Среда

- OpenJDK и `javac` 25.0.4.1;
- Gradle Wrapper 9.1.0;
- Kotlin Gradle Plugin и stdlib 2.4.10;
- Linux amd64;
- первая подготовка Kotlin-зависимостей выполнена с сетью, последующие проверки — с `--offline`.

## Ожидаемые результаты

| Проверка | Ожидаемый результат |
|---|---|
| обычный `NutritionPlan` | точная подпись из Kotlin со всеми компонентами Java-record |
| Java-вызов с `null` | `IllegalArgumentException` и сообщение `План питания обязателен` |
| граничный план | идентификатор `0`, старая дата и `NEEDS_REVIEW` не теряются |
| JVM-сигнатура | Java видит `public static String format(NutritionPlan)` |
| `installDist` | Kotlin-класс и `kotlin-stdlib-2.4.10.jar` включены |
| запуск из дистрибутива | Java-клиент работает только с JAR из `installDist/lib` |
| полная регрессия | прежние тесты, JavaFX smoke и app-image не нарушены |

## Фактические результаты

| Проверка | Фактический результат |
|---|---|
| первая автономная подготовка | ожидаемо остановилась: Kotlin Plugin отсутствовал в кэше |
| подготовка с сетью | Kotlin Plugin 2.4.10 и зависимости загружены |
| первая компиляция | обнаружила ошибочное имя компонента record `effectiveDate`; исправлено на `effectiveFrom` |
| первый модульный Java compile | выявил отсутствие Kotlin output в JPMS compile; добавлен `--patch-module` |
| `testInterop` | 4 сценария, 0 отказов |
| `verifyKotlinDistribution` | Kotlin-класс и stdlib найдены; Java-клиент из `installDist` завершился с кодом 0 |
| `runLessonDemo` | две ожидаемые строки получены без предупреждений JavaFX |
| `./gradlew --offline clean check runLessonDemo` | успешно; 152 тестовых вызова в основном наборе, 0 отказов; interop-демонстрация совпала с ожиданием |
| `verifyDistribution` | успешно; Kotlin stdlib найден в app-image, оба внешних JavaFX-запуска прошли |
| smoke-запуск | успешно; FXML, CSS, конфигурация и SQLite доступны |

Краткие фактические выводы находятся в [`docs/evidence`](evidence/). Промежуточные ошибки сохранены как часть инженерного разбора: обе относились к реальной границе смешанной сборки и были устранены до приёмки.

## Итог

Критерии практики 23 выполнены. Java вызывает Kotlin по проверенной статической сигнатуре, Kotlin читает Java-record, `null` получает явный отказ, а Kotlin runtime присутствует и работает в `installDist` и app-image. Остальные слои приложения сохранили Java-реализацию и прошли регрессию.
