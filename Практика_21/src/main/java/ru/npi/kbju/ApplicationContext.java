package ru.npi.kbju;

import java.nio.file.Path;
import java.time.LocalDate;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.application.observation.ObservedNutritionPlanService;
import ru.npi.kbju.application.observation.OperationCallMetrics;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;
import ru.npi.kbju.infrastructure.AppConfig;
import ru.npi.kbju.infrastructure.SqliteNutritionPlanRepository;

/** Собирает прежний сервис с файловым SQLite-адаптером и наблюдающим proxy. */
public final class ApplicationContext implements AutoCloseable {
    private final NutritionPlanRepository repository;
    private final Path databasePath;
    private final OperationCallMetrics metrics;
    private final NutritionPlanOperations service;

    private ApplicationContext(Path databasePath) {
        var sqliteRepository = new SqliteNutritionPlanRepository(databasePath);
        this.repository = sqliteRepository;
        this.databasePath = sqliteRepository.databasePath();
        try {
            seedWhenEmpty();
            this.metrics = new OperationCallMetrics();
            this.service = ObservedNutritionPlanService.create(
                    new NutritionPlanService(repository), metrics);
        } catch (RuntimeException exception) {
            try {
                sqliteRepository.close();
            } catch (RuntimeException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    /** Открывает постоянную базу в настроенном каталоге данных. */
    public static ApplicationContext createDefault() {
        var config = AppConfig.load();
        config.prepareDirectories();
        return new ApplicationContext(config.databasePath());
    }

    /** Открывает отдельный файл; используется воспроизводимыми проверками. */
    public static ApplicationContext create(Path databasePath) {
        return new ApplicationContext(databasePath);
    }

    private void seedWhenEmpty() {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        try {
            repository.save(new NutritionPlan(
                    0, "Снижение массы", LocalDate.of(2026, 10, 1),
                    NutritionPlan.Status.DRAFT));
        } catch (DuplicatePlanException exception) {
            throw new IllegalStateException(
                    "Начальный план неожиданно конфликтует с пустой базой", exception);
        }
    }

    /** Возвращает интерфейс прикладных операций без раскрытия адаптера. */
    public NutritionPlanOperations service() {
        return service;
    }

    /** Возвращает метрики отмеченных предметных операций текущего контекста. */
    public OperationCallMetrics metrics() {
        return metrics;
    }

    /** Возвращает путь открытой базы для диагностики. */
    public Path databasePath() {
        return databasePath;
    }

    /** Освобождает принадлежащее SQLite-адаптеру соединение. */
    @Override
    public void close() {
        repository.close();
    }
}
