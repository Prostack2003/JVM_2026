# Матрица проверок практики 17

| Сценарий | Ожидаемый результат | Доказательство |
|---|---|---|
| обычная допустимая запись с `id=1` | имя очищено, все компоненты сохранены | `validPlanNormalizesNameAndPreservesEveryComponent` |
| `id=0` до сохранения | запись создана | `acceptsZeroIdentifierBeforePersistence` |
| `id=-1` | ошибка поля `ID` | `rejectsNegativeIdentifier` |
| шесть вариантов отсутствующего/пустого имени | каждый отклонён как поле `NAME` | параметризованный `rejectsEveryBlankNameVariant` |
| имя длиной 60 | запись создана | `acceptsNameAtExactLengthBoundary` |
| имя длиной 61 | ошибка поля `NAME` | `rejectsNameOneCharacterBeyondMaximum` |
| дата отсутствует | ошибка поля `EFFECTIVE_FROM` | `rejectsMissingEffectiveDate` |
| статус отсутствует | ошибка поля `STATUS` | `rejectsMissingStatus` |
| `DRAFT` меняется на `ACTIVE` | новый record; исходный и остальные компоненты сохранены | `withStatusCreatesReplacementAndKeepsOriginalRecord` |
| `withStatus(null)` | отказ без изменения исходника | `withStatusRejectsNullInsteadOfCreatingInvalidReplacement` |
| временно удалён `isBlank()` | пять параметров делают профиль красным | `docs/evidence/mutation-failed.txt` |
| правило восстановлено | два чистых профильных прогона совпадают | `docs/evidence/tests.txt` |
| регрессия практик 1–16 | полный `clean check` зелёный | `docs/check-protocol.md` |
| автономная сборка | зависимости берутся из подготовленного кэша | запуск с `--offline` |
