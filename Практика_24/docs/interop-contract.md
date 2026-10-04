# Контракт взаимодействия Java и Kotlin

## Положительная граница

Java-клиент вызывает:

```java
String caption = NutritionPlanCaption.format(plan);
```

Kotlin объявляет `object NutritionPlanCaption` и метод `@JvmStatic format`. JVM-сигнатура принимает `ru.npi.kbju.domain.NutritionPlan` и возвращает `java.lang.String`. Рефлексивный тест подтверждает модификаторы `public static`, имя метода, тип параметра и возвращаемый тип.

Kotlin читает Java-record через компоненты `id`, `name`, `effectiveFrom` и `status`. Форматировщик не меняет record и не вызывает UI, сервис или репозиторий.

## Null-контракт

Параметр Kotlin имеет тип `NutritionPlan?`, потому что Java-клиент технически может передать `null`. Первая операция метода:

```kotlin
val requiredPlan = requireNotNull(plan) { "План питания обязателен" }
```

Java наблюдает `IllegalArgumentException` с указанным сообщением. Контракт не зависит от текста внутренней проверки Kotlin compiler и не использует `!!`.

## Стабильный формат

```text
План #<id>: <name> [<status>], действует с <effectiveFrom>
```

Дата выводится в ISO-формате `yyyy-MM-dd`, состояние берётся из Java-enum. Формат не содержит текущего времени и не зависит от локали, поэтому тест воспроизводим.

## За пределами компонента

- проверка инвариантов остаётся в Java-record `NutritionPlan`;
- создание и сохранение планов остаётся в Java-сервисе и репозитории;
- компонент не получает доступ к JavaFX или SQLite;
- полная миграция приложения на Kotlin не входит в практику.
