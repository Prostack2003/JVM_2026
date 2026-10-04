# Матрица прослеживаемости выпуска 24.0.0

| Код | Статус | Требование | Реализация | Проверка, команда и доказательство |
|---|---|---|---|---|
| ПР-01 | ПРОЙДЕНО | План добавляется из FXML-формы | `MainController.addPlan`, `NutritionPlanService`, `SqliteNutritionPlanRepository.save` | `JavaFxUserJourneyTest.userCanAddPlanThroughRealFxmlControls`; `./gradlew check`; `docs/evidence/release-tests.txt` |
| ПР-02 | ПРОЙДЕНО | Пустое название не сохраняется | `MainController.addPlan`, `NutritionPlan` | `JavaFxUserJourneyTest.blankNameShowsFieldErrorAndDoesNotSave`; `./gradlew check`; `docs/evidence/release-tests.txt` |
| ПР-03 | ПРОЙДЕНО | SQLite сохраняет запись после перезапуска app-image | `ApplicationContext`, `AppConfig`, `SqliteNutritionPlanRepository` | интеграционные тесты и `verifyPackagedLaunch`; `docs/evidence/release-distribution.txt` |
| ПР-04 | ПРОЙДЕНО | Недоступный справочник не подменяет подтверждённые данные | `ResilientProductCatalogService`, `FileProductCatalogCache` | `OfflineCatalogIntegrationTest`, `runOfflineCatalogAcceptance`; `docs/evidence/release-offline.txt` |
| ПР-05 | ПРОЙДЕНО | Копия восстанавливается только в новый проверенный файл | `SqliteBackupService` | `SqliteBackupServiceTest`, `runBackupRecoveryAcceptance`; `docs/evidence/release-recovery.txt` |

## Дополнительные выпускные свойства

| Свойство | Доказательство | Статус |
|---|---|---|
| Kotlin-класс вызывается из Java и runtime включён в поставку | `KotlinInteropTest`, `verifyKotlinDistribution` | ПРОЙДЕНО |
| В JAR есть FXML, CSS, миграции, service descriptor и манифест выпуска | `verifyJarContents`, `verifyPackagedResources` | ПРОЙДЕНО |
| App-image работает вне исходного каталога со встроенной Java | `verifyPackagedLaunch` | ПРОЙДЕНО на Linux/amd64 |
| Запуск другим человеком без устных подсказок | инструкция подготовлена, фактический второй участник недоступен | НЕ ПРОВЕРЕНО |

Статус `НЕ ПРОВЕРЕНО` означает отсутствие фактического результата, а не отрицательный результат выполненной проверки.
