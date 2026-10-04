package ru.npi.kbju.lesson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import ru.npi.kbju.infrastructure.AppConfig;
import ru.npi.kbju.infrastructure.ApplicationLog;

/** Показывает разрешённые пути и безопасное диагностическое событие. */
public final class ConfigurationDemo {
    private static final String SECRET = "KBJU-SECRET-TOKEN-DO-NOT-LOG";
    private static final String PRIVATE_PLAN = "Личный план пациента";

    private ConfigurationDemo() {
    }

    /** Выполняет штатный и контролируемый диагностический сценарии. */
    public static void main(String[] args) throws IOException {
        var config = AppConfig.load();
        config.prepareDirectories();

        final String eventId;
        try (var log = ApplicationLog.open(config.logDirectory())) {
            log.recordApplicationStarted(config);
            var sensitiveCause = new IOException(
                    "token=" + SECRET + "; plan=" + PRIVATE_PLAN);
            eventId = log.recordFailure(
                    ApplicationLog.Operation.PLAN_SAVE,
                    ApplicationLog.FailureReason.STORAGE_UNAVAILABLE,
                    sensitiveCause
            );
        }

        var journal = readJournal(config);
        if (journal.contains(SECRET) || journal.contains(PRIVATE_PLAN)) {
            throw new IllegalStateException("Журнал раскрыл запрещённые данные");
        }

        System.out.println("Источник настроек: " + config.source());
        System.out.println("Корень данных: " + config.dataRoot());
        System.out.println("База: " + config.databasePath());
        System.out.println("Журнал: " + config.logDirectory());
        System.out.println("Резервные копии: " + config.backupDirectory());
        System.out.println("Пользователю: не удалось сохранить план; проверьте доступ к каталогу данных и повторите. Код события: " + eventId);
        System.out.println("Диагностика: " + journal.lines()
                .filter(line -> line.contains("event="))
                .collect(Collectors.joining(" | ")));
        System.out.println("Секреты и содержимое плана в журнале: отсутствуют");
    }

    private static String readJournal(AppConfig config) throws IOException {
        try (var paths = Files.list(config.logDirectory())) {
            var logs = paths
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted()
                    .toList();
            var content = new StringBuilder();
            for (var path : logs) {
                content.append(Files.readString(path, StandardCharsets.UTF_8));
            }
            return content.toString();
        }
    }
}
