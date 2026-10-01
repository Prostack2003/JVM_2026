package ru.npi.kbju.lesson;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ru.npi.kbju.application.NutritionPlanOperations;
import ru.npi.kbju.application.NutritionPlanService;
import ru.npi.kbju.application.observation.ObservedNutritionPlanService;
import ru.npi.kbju.application.observation.OperationCallMetrics;
import ru.npi.kbju.application.observation.OperationCatalog;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.PlanValidationException;
import ru.npi.kbju.infrastructure.MemoryNutritionPlanRepository;

/** Воспроизводимый показ аннотаций, рефлексии и динамического proxy практики 10. */
public final class ReflectionDemo {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);

    private ReflectionDemo() {
    }

    /** Выполняет приёмочную матрицу без JavaFX и пользовательских данных. */
    public static void main(String[] args) throws Exception {
        System.out.println("Публичных методов контракта: "
                + NutritionPlanOperations.class.getMethods().length);
        for (var operation : OperationCatalog.describe(NutritionPlanOperations.class)) {
            System.out.println(operation.methodName() + " — " + operation.title());
        }
        System.out.println("all — пропущен: аннотация отсутствует");

        var directRepository = new MemoryNutritionPlanRepository(List.of());
        var observedRepository = new MemoryNutritionPlanRepository(List.of());
        try {
            NutritionPlanOperations direct = new NutritionPlanService(
                    directRepository, FIXED_CLOCK);
            var metrics = new OperationCallMetrics();
            NutritionPlanOperations observed = ObservedNutritionPlanService.create(
                    new NutritionPlanService(observedRepository, FIXED_CLOCK), metrics);

            var directResult = direct.addPlan(
                    "Поддержание массы", LocalDate.of(2026, 10, 2),
                    NutritionPlan.Status.DRAFT);
            var proxyResult = observed.addPlan(
                    "Поддержание массы", LocalDate.of(2026, 10, 2),
                    NutritionPlan.Status.DRAFT);
            require(directResult.equals(proxyResult),
                    "Proxy изменил результат создания плана");
            observed.changeStatus(proxyResult.id(), NutritionPlan.Status.ACTIVE);

            require(observed.equals(observed), "equals proxy должен быть рефлексивным");
            observed.hashCode();
            observed.toString();
            require(metrics.total() == 2, "Должно учитываться ровно два вызова");
            System.out.println("Прямой и proxied-вызов: результаты совпали");
            System.out.println("Вызовов отмеченных операций: " + metrics.total());

            verifyOriginalFailureIsPreserved();
            System.out.println("Бизнес-правила остались в сервисе и предметной модели");
        } finally {
            directRepository.close();
            observedRepository.close();
        }
    }

    private static void verifyOriginalFailureIsPreserved() throws Exception {
        var repository = new MemoryNutritionPlanRepository(List.of());
        try {
            var failureMetrics = new OperationCallMetrics();
            var observed = ObservedNutritionPlanService.create(
                    new NutritionPlanService(repository, FIXED_CLOCK), failureMetrics);
            try {
                observed.addPlan("   ", LocalDate.of(2026, 10, 2),
                        NutritionPlan.Status.DRAFT);
                throw new AssertionError("Некорректное название должно быть отклонено");
            } catch (PlanValidationException expected) {
                require(failureMetrics.total() == 1,
                        "Неуспешная попытка предметной операции тоже является вызовом");
                System.out.println("Ошибка сервиса передана без оболочки: "
                        + expected.getClass().getSimpleName());
            }
        } finally {
            repository.close();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
