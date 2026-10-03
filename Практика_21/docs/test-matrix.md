# Матрица проверок практики 21

| Вход или действие | Ожидаемый результат | Доказательство |
|---|---|---|
| `clean jar` | свежий JAR в `build/libs` | Gradle и `jar tf` |
| `verifyJarContents` | обязательные классы, FXML, CSS и descriptor присутствуют | проверка белого списка ZIP entries |
| descriptor | ровно два полных имени поставщиков | содержимое `META-INF/services/...` |
| `runLessonDemo` | полный и компактный отчёты, `Поставщиков: 2` | запуск из основного JAR |
| одинаковая суточная сводка | поставщики возвращают разные непустые строки | `NutritionReportPluginTest` |
| отрицательные калории, нулевая цель, `NaN` | данные отклонены до поставщика | `summaryRejectsInvalidCaloriesAndNutrients` |
| JAR без descriptor | ненулевой код и сообщение об отсутствии поставщиков | `verifyMissingProviderFailure` |
| полная регрессия | прежние функции не нарушены | `./gradlew clean check` |
| автономная проверка | сеть не требуется | `./gradlew --offline clean check` |
| smoke-запуск | FXML, CSS, конфигурация и SQLite доступны | `./gradlew --offline run --args="--smoke"` |
