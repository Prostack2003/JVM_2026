# Матрица проверок практики 20

| Вход или действие | Ожидаемый результат | Доказательство |
|---|---|---|
| открытая рабочая SQLite-база | согласованный снимок открывается отдельно | `liveDatabaseSnapshotOpensSeparatelyAndContainsExpectedRecords` |
| `PRAGMA integrity_check` снимка | единственный результат `ok` | `BackupSnapshot.inspection` |
| изменение после снимка | старый снимок не содержит новую запись | `laterChangeDoesNotAppearInOlderSnapshot` |
| восстановление в новый путь | поля и устойчивые ID совпадают | `restoresOnlyToNewPathAndPreservesEveryField` |
| повреждённый отдельный файл | `INTEGRITY_FAILED`, цель отсутствует | `damagedFileIsRejectedWithoutChangingWorkingDatabaseOrGoodSnapshot` |
| существующая цель восстановления | отказ без изменения её байтов | `existingRestoreTargetIsRejectedAndItsBytesStayUntouched` |
| повторное имя снимка | прежний снимок не перезаписан | `repeatedBackupTargetIsRejectedWithoutOverwritingFirstSnapshot` |
| база без журнала миграций | `UNSUPPORTED_SCHEMA`, цель отсутствует | `schemaWithoutMigrationHistoryIsRejectedBeforeTargetCreation` |
| источник совпадает с целью | ранний `SAME_PATH` | `sourceAndTargetMustDifferEvenWhenBackupIsValid` |
| апостроф в пути копии | SQLite-снимок создаётся и проверяется | `sqliteSnapshotEscapesApostropheInTargetPath` |
| полный демонстрационный маршрут | снимок, позднее изменение, restore и отказ наблюдаемы | `runLessonDemo` |
| полная регрессия | прежние функции не нарушены | `./gradlew clean check` |
| автономный прогон | сеть не требуется | `./gradlew --offline clean check` |
