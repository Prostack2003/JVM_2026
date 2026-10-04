# Портфель проверок выпуска 24.0.0

| Уровень | Что подтверждает |
|---|---|
| domain | инварианты неизменяемых моделей и граничные значения |
| application | прикладные сценарии, фоновые операции, concurrency и observation |
| infrastructure | SQLite, миграции, настройки, журнал, backup/restore |
| integration | HTTP/JSON, retry, TTL, атомарный кэш и автономные отказы |
| UI | FXML, CSS, реальный пользовательский маршрут и доступность |
| interop | Java → Kotlin, Java record из Kotlin, null-контракт и JVM-сигнатура |
| packaging | JAR, `installDist`, service descriptor, app-image и встроенная Java |
| release | манифест, матрица, документы, clean-room запуск и два архива с SHA-256 |

`check` покрывает код и содержимое JAR. `releaseAcceptance` добавляет фактические демонстрации, app-image вне проекта и полный комплект поставки.
