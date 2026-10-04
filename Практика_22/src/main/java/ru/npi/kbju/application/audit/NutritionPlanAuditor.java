package ru.npi.kbju.application.audit;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет порционную проверку планов без зависимости от JavaFX. */
public final class NutritionPlanAuditor {
    /** Число отдельных проверок каждого плана. */
    public static final int CHECKS_PER_PLAN = 6;

    /** Подменяемая пауза делает длительную операцию детерминированно тестируемой. */
    @FunctionalInterface
    public interface Pause {
        /** Ожидает между короткими порциями работы. */
        void waitFor(Duration duration) throws InterruptedException;
    }

    private static final String[] CHECK_NAMES = {
        "идентификатор",
        "нормализация названия",
        "длина названия",
        "дата начала",
        "состояние",
        "уникальность ID"
    };

    private final Duration stepDelay;
    private final Pause pause;

    /** Принимает длительность шага и механизм ожидания. */
    public NutritionPlanAuditor(Duration stepDelay, Pause pause) {
        this.stepDelay = Objects.requireNonNull(stepDelay, "Задержка шага обязательна");
        if (stepDelay.isNegative()) {
            throw new IllegalArgumentException("Задержка шага не может быть отрицательной");
        }
        this.pause = Objects.requireNonNull(pause, "Механизм паузы обязателен");
    }

    /**
     * Проверяет неизменяемый снимок и возвращает итог только после всех шагов.
     *
     * @throws CancellationException если между порциями получен запрос отмены
     * @throws InterruptedException если блокирующая пауза прервана
     */
    public AuditResult audit(
            List<NutritionPlan> plans,
            BooleanSupplier cancellationRequested,
            Consumer<AuditProgress> progressListener
    ) throws InterruptedException {
        var snapshot = List.copyOf(Objects.requireNonNull(plans, "Снимок планов обязателен"));
        Objects.requireNonNull(cancellationRequested, "Проба отмены обязательна");
        Objects.requireNonNull(progressListener, "Слушатель прогресса обязателен");

        var seenIds = new HashSet<Long>();
        int completed = 0;
        int total = totalChecks(snapshot.size());
        for (var plan : snapshot) {
            Objects.requireNonNull(plan, "Снимок не должен содержать null");
            for (var checkIndex = 0; checkIndex < CHECKS_PER_PLAN; checkIndex++) {
                stopWhenCancelled(cancellationRequested);
                pause.waitFor(stepDelay);
                stopWhenCancelled(cancellationRequested);
                check(plan, checkIndex, seenIds);
                completed++;
                progressListener.accept(new AuditProgress(
                        completed, total, CHECK_NAMES[checkIndex]));
            }
        }

        int active = (int) snapshot.stream()
                .filter(plan -> plan.status() == NutritionPlan.Status.ACTIVE)
                .count();
        int needsReview = (int) snapshot.stream()
                .filter(plan -> plan.status() == NutritionPlan.Status.NEEDS_REVIEW)
                .count();
        return new AuditResult(snapshot.size(), active, needsReview, completed);
    }

    /** Возвращает число коротких порций работы для указанного снимка. */
    public static int totalChecks(int planCount) {
        if (planCount < 0) {
            throw new IllegalArgumentException("Число планов не может быть отрицательным");
        }
        return Math.multiplyExact(planCount, CHECKS_PER_PLAN);
    }

    private static void stopWhenCancelled(BooleanSupplier cancellationRequested) {
        if (cancellationRequested.getAsBoolean()) {
            throw new CancellationException(
                    "Аудит отменён в безопасной точке между проверками");
        }
    }

    private static void check(NutritionPlan plan, int checkIndex, HashSet<Long> seenIds) {
        switch (checkIndex) {
            case 0 -> require(plan.id() > 0,
                    "План без устойчивого ID: " + plan.name());
            case 1 -> require(plan.name().equals(plan.name().strip()),
                    "Название плана не нормализовано: " + plan.id());
            case 2 -> require(plan.name().length() <= NutritionPlan.MAX_NAME_LENGTH,
                    "Название плана слишком длинное: " + plan.id());
            case 3 -> require(plan.effectiveFrom() != null,
                    "У плана нет даты начала: " + plan.id());
            case 4 -> require(plan.status() != null,
                    "У плана нет состояния: " + plan.id());
            case 5 -> require(seenIds.add(plan.id()),
                    "Обнаружен повтор ID: " + plan.id());
            default -> throw new IllegalArgumentException("Неизвестная проверка: " + checkIndex);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AuditExecutionException(message);
        }
    }
}
