# Портфель проверок практики 23

| Риск | Самая дешёвая достаточная граница | Проверка |
|---|---|---|
| Java не видит Kotlin-метод | Java compilation и рефлексия | точная JVM-сигнатура |
| Kotlin не видит компоненты Java-record | Kotlin compilation и строковый результат | положительный interop-тест |
| Java передаёт `null` как platform type | вызов границы из Java | точный тип и текст отказа |
| форматировщик незаметно меняет вход | повторное чтение immutable record | значения после вызова |
| граничный `id=0` теряется | предметный тест | отдельный boundary-сценарий |
| Kotlin-класс не попал в JAR | ZIP entries | `verifyKotlinDistribution` |
| stdlib доступен только из Gradle cache | инвентаризация `installDist/lib` | поиск `kotlin-stdlib*.jar` |
| дистрибутив компилируется, но не запускается | отдельный Java process | classpath только из `installDist/lib` |
| JPMS не включает Kotlin output | чистая компиляция | `--patch-module` и `clean check` |
| Kotlin нарушил прежнее приложение | накопительная регрессия | полный `test`, smoke и app-image |

Наличие `.kt` файла или зелёная Kotlin compilation сами по себе не закрывают риски Java-вызова и поставки runtime.
