package ru.npi.kbju;

import java.time.LocalDate;
import java.util.List;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.application.observation.ObservedNutritionPlanService;
import ru.npi.kbju.application.observation.OperationCallMetrics;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Единственное место сборки конкретного адаптера и прикладного сервиса. */
public final class ApplicationContext implements AutoCloseable {
    private final NutritionPlanRepository repository;
    private final OperationCallMetrics metrics;
    private final NutritionPlanOperations service;

    private ApplicationContext(NutritionPlanRepository repository) {
        this.repository = repository;
        this.metrics = new OperationCallMetrics();
        this.service = ObservedNutritionPlanService.create(
                new NutritionPlanService(repository), metrics);
    }

    /** Создаёт независимый контекст с одной демонстрационной записью. */
    public static ApplicationContext createDefault() {
        var initialPlan = new NutritionPlan(
                1, "Снижение массы", LocalDate.of(2026, 10, 1),
                NutritionPlan.Status.DRAFT);
        return new ApplicationContext(
                new MemoryNutritionPlanRepository(List.of(initialPlan)));
    }

    /** Возвращает готовый сервис; конкретный репозиторий наружу не раскрывается. */
    public NutritionPlanOperations service() {
        return service;
    }

    /** Возвращает метрики отмеченных предметных операций текущего контекста. */
    public OperationCallMetrics metrics() {
        return metrics;
    }

    /** Освобождает ресурсы выбранного адаптера. */
    @Override
    public void close() {
        repository.close();
    }
}
