package ru.npi.kbju.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import ru.npi.kbju.domain.NutritionPlan;

/** Выполняет операции текущего сеанса с планами питания в памяти процесса. */
public final class NutritionPlanService {
    private final List<NutritionPlan> plans;
    private final Clock clock;
    private long nextId;

    /**
     * Создаёт сервис с начальными данными и источником текущей даты.
     *
     * @param initialPlans начальные планы
     * @param clock источник времени, заменяемый в тестах
     */
    public NutritionPlanService(Collection<NutritionPlan> initialPlans, Clock clock) {
        Objects.requireNonNull(initialPlans, "Начальные планы обязательны");
        this.clock = Objects.requireNonNull(clock, "Источник времени обязателен");
        this.plans = new ArrayList<>(initialPlans);
        this.plans.forEach(Objects::requireNonNull);
        this.nextId = Math.addExact(
                this.plans.stream().mapToLong(NutritionPlan::id).max().orElse(0),
                1
        );
    }

    /**
     * Создаёт сервис с одной исходной записью для первого экрана.
     *
     * @return сервис с предсказуемым начальным состоянием
     */
    public static NutritionPlanService createDefault() {
        var initialPlan = new NutritionPlan(
                1,
                "Снижение массы",
                LocalDate.of(2026, 10, 1),
                NutritionPlan.Status.DRAFT
        );
        return new NutritionPlanService(List.of(initialPlan), Clock.systemDefaultZone());
    }

    /**
     * Возвращает отделённый неизменяемый снимок планов текущего сеанса.
     *
     * @return снимок планов
     */
    public List<NutritionPlan> plans() {
        return List.copyOf(plans);
    }

    /**
     * Добавляет черновик после проверки предметной моделью.
     *
     * @param name название плана
     * @return добавленная запись
     */
    public NutritionPlan addDraft(String name) {
        var plan = new NutritionPlan(
                nextId,
                name,
                LocalDate.now(clock),
                NutritionPlan.Status.DRAFT
        );
        plans.add(plan);
        nextId = Math.addExact(nextId, 1);
        return plan;
    }
}
