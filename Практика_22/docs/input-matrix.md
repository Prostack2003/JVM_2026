# Матрица входов для NutritionPlan

Ожидания записаны до запуска тестов и получены из контракта предметной модели.

| Правило | Вход | Ожидаемый результат | Тест |
|---|---|---|---|
| допустимая запись и нормализация | `id=1`, имя с крайними пробелами, фиксированная дата, `DRAFT` | запись создана, имя очищено, остальные компоненты сохранены | `validPlanNormalizesNameAndPreservesEveryComponent` |
| нулевой идентификатор допустим до сохранения | `id=0` при остальных корректных полях | запись создана с `id=0` | `acceptsZeroIdentifierBeforePersistence` |
| идентификатор неотрицателен | `id=-1` | `PlanValidationException`, поле `ID` | `rejectsNegativeIdentifier` |
| имя обязательно | `null`, `""`, пробелы, табуляция, перевод строки | для каждого значения `PlanValidationException`, поле `NAME` | `rejectsEveryBlankNameVariant` |
| длина имени не больше 60 | ровно 60 символов | запись создана | `acceptsNameAtExactLengthBoundary` |
| длина имени не больше 60 | 61 символ | `PlanValidationException`, поле `NAME` | `rejectsNameOneCharacterBeyondMaximum` |
| дата обязательна | `effectiveFrom=null` | `PlanValidationException`, поле `EFFECTIVE_FROM` | `rejectsMissingEffectiveDate` |
| статус обязателен | `status=null` | `PlanValidationException`, поле `STATUS` | `rejectsMissingStatus` |
| смена состояния неизменяема | `DRAFT.withStatus(ACTIVE)` | новый record; ID, имя и дата сохранены; исходный остаётся `DRAFT` | `withStatusCreatesReplacementAndKeepsOriginalRecord` |
| недопустимый переход не создаёт запись | `withStatus(null)` | `PlanValidationException`, исходная запись не изменена | `withStatusRejectsNullInsteadOfCreatingInvalidReplacement` |

Тесты создают новую запись внутри каждого сценария и используют `LocalDate.of(2026, 10, 1)`. Они не зависят от порядка выполнения, текущей даты, базы данных или JavaFX.
