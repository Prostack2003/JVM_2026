# Манифест поставки 24.0.0

## App-image

| Поле | Значение |
|---|---|
| имя | `KbjuDiary` |
| версия | `24.0.0` |
| проект | `jvm-practice-24` |
| основной JAR | `jvm-practice-24-24.0.0.jar` |
| подтверждённая платформа | Linux/amd64 |
| встроенная Java | `KbjuDiary/lib/runtime/` |
| приложение | `KbjuDiary/lib/app/` |

Основной JAR содержит FXML, CSS, миграции, service descriptor, Kotlin-класс и `release-manifest.txt`. Kotlin stdlib находится рядом с приложением. Файлы `.db` и `.log` в образ не входят.

## Архивы

```text
build/distributions/KbjuDiary-24.0.0-linux-amd64.tar.gz
build/distributions/KbjuDiary-24.0.0-release-bundle-linux-amd64.tar.gz
```

Первый архив содержит app-image. Второй содержит app-image, исходники, документацию и доказательства. Для каждого создаётся отдельный файл `.sha256`.
