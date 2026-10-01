package ru.npi.kbju.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.npi.kbju.domain.NutritionPlan;

/** Управляет наблюдаемым набором планов питания текущего сеанса. */
public final class NutritionPlanService {
    private final ObservableList<NutritionPlan> plans;
    private final ObservableList<NutritionPlan> readOnlyPlans;
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
        initialPlans.forEach(Objects::requireNonNull);
        this.plans = FXCollections.observableArrayList(initialPlans);
        this.readOnlyPlans = FXCollections.unmodifiableObservableList(plans);
        this.nextId = Math.addExact(
                plans.stream().mapToLong(NutritionPlan::id).max().orElse(0),
                1
        );
    }

    /**
     * Создаёт сервис с одной исходной записью для воспроизводимого экрана.
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
     * Возвращает неизменяемое представление того же наблюдаемого списка.
     *
     * @return список, уведомляющий таблицу и привязки об изменениях
     */
    public ObservableList<NutritionPlan> plans() {
        return readOnlyPlans;
    }

    /**
     * Добавляет уникальный по названию черновик.
     *
     * @param name название плана
     * @return добавленная запись
     */
    public NutritionPlan addDraft(String name) {
        var normalizedName = normalizeName(name);
        if (containsName(normalizedName)) {
            throw new IllegalArgumentException("План с таким названием уже существует");
        }
        var plan = new NutritionPlan(
                nextId,
                normalizedName,
                LocalDate.now(clock),
                NutritionPlan.Status.DRAFT
        );
        plans.add(plan);
        nextId = Math.addExact(nextId, 1);
        return plan;
    }

    /**
     * Удаляет существующую запись.
     *
     * @param plan удаляемый план
     * @return {@code true}, если запись находилась в списке
     */
    public boolean remove(NutritionPlan plan) {
        return plans.remove(Objects.requireNonNull(plan, "Удаляемый план обязателен"));
    }

    /**
     * Заменяет неизменяемую запись новой записью с другим состоянием.
     *
     * @param plan существующая запись
     * @param newStatus новое состояние
     * @return запись, которой заменён исходный элемент
     */
    public NutritionPlan changeStatus(NutritionPlan plan, NutritionPlan.Status newStatus) {
        Objects.requireNonNull(plan, "Изменяемый план обязателен");
        Objects.requireNonNull(newStatus, "Новое состояние обязательно");
        var index = plans.indexOf(plan);
        if (index < 0) {
            throw new IllegalArgumentException("План отсутствует в текущем списке");
        }
        var replacement = plan.withStatus(newStatus);
        plans.set(index, replacement);
        return replacement;
    }

    private boolean containsName(String name) {
        var key = name.toLowerCase(Locale.ROOT);
        return plans.stream()
                .map(NutritionPlan::name)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(key::equals);
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Название обязательно");
        }
        return value.trim();
    }
}
