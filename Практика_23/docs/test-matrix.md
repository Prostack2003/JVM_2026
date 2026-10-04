# Матрица проверок практики 23

| Вход или действие | Ожидаемый результат | Доказательство |
|---|---|---|
| обычный план `id=23` | точная подпись с именем, статусом и датой | `javaCallsKotlinFormatterWhichReadsJavaRecord` |
| исходное имя с пробелами | Kotlin получает уже нормализованный Java-record | тот же тест и `plan.name()` |
| `NutritionPlanCaption.format(null)` | точный `IllegalArgumentException` | `nullFromJavaFollowsExplicitKotlinContract` |
| `id=0`, дата 2000-01-01, `NEEDS_REVIEW` | значения сохранены в строке | `boundaryPlanKeepsZeroIdentifierAndReviewStatus` |
| рефлексия Java | `public static format(NutritionPlan): String` | `javaSeesPredictablePublicStaticJvmSignature` |
| чистый `test` | весь накопительный набор проходит | Gradle test report |
| `installDist` | Kotlin-класс находится в основном JAR | ZIP-проверка `verifyKotlinDistribution` |
| `installDist/lib` | присутствует `kotlin-stdlib-2.4.10.jar` | инвентаризация задачи |
| Java-процесс из `installDist` | две точные строки, код 0 | `verifyKotlinDistribution` |
| `runLessonDemo` | тот же результат через Gradle | фактический вывод задачи |
| `verifyDistribution` | app-image включает Kotlin и проходит прежний маршрут | структурная и фактическая проверка |
| smoke-запуск | FXML, CSS и SQLite работают | `run --args="--smoke"` |
