package ru.npi.kbju;

import java.time.LocalDate;
import java.util.List;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Единственное место сборки конкретного адаптера и прикладного сервиса. */
public final class ApplicationContext implements AutoCloseable {
    private final NutritionPlanRepository repository;
    private final NutritionPlanService service;

    private ApplicationContext(NutritionPlanRepository repository) {
        this.repository = repository;
        this.service = new NutritionPlanService(repository);
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
    public NutritionPlanService service() {
        return service;
    }

    /** Освобождает ресурсы выбранного адаптера. */
    @Override
    public void close() {
        repository.close();
    }
}
