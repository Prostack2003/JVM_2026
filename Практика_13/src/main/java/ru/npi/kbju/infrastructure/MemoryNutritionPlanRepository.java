package ru.npi.kbju.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;

/** Потокобезопасный адаптер порта, хранящий независимое состояние в памяти. */
public final class MemoryNutritionPlanRepository implements NutritionPlanRepository {
    private final List<NutritionPlan> plans = new ArrayList<>();
    private long nextId = 1;

    /** Создаёт пустое независимое хранилище. */
    public MemoryNutritionPlanRepository() {
    }

    /** Создаёт хранилище с проверенным снимком начальных данных. */
    public MemoryNutritionPlanRepository(Collection<NutritionPlan> initialPlans) {
        Objects.requireNonNull(initialPlans, "Начальные планы обязательны");
        for (var plan : initialPlans) {
            Objects.requireNonNull(plan, "Начальный план обязателен");
            if (plan.id() == 0) {
                throw new IllegalArgumentException("Начальный план должен иметь устойчивый ID");
            }
            if (hasDuplicateName(plan)) {
                throw new IllegalArgumentException(
                        "В начальных данных повторяется план: " + plan.name());
            }
            plans.add(plan);
            nextId = Math.max(nextId, Math.addExact(plan.id(), 1));
        }
    }

    @Override
    public synchronized List<NutritionPlan> findAll() {
        return List.copyOf(plans);
    }

    @Override
    public synchronized Optional<NutritionPlan> findById(long id) {
        return plans.stream().filter(plan -> plan.id() == id).findFirst();
    }

    @Override
    public synchronized NutritionPlan save(NutritionPlan plan)
            throws DuplicatePlanException {
        Objects.requireNonNull(plan, "Сохраняемый план обязателен");
        if (hasDuplicateName(plan)) {
            throw new DuplicatePlanException(plan.name());
        }

        if (plan.id() == 0) {
            var saved = new NutritionPlan(
                    nextId, plan.name(), plan.effectiveFrom(), plan.status());
            plans.add(saved);
            nextId = Math.addExact(nextId, 1);
            return saved;
        }

        for (var index = 0; index < plans.size(); index++) {
            if (plans.get(index).id() == plan.id()) {
                plans.set(index, plan);
                return plan;
            }
        }
        throw new NoSuchElementException("План с ID " + plan.id() + " не найден");
    }

    @Override
    public synchronized boolean deleteById(long id) {
        return plans.removeIf(plan -> plan.id() == id);
    }

    private boolean hasDuplicateName(NutritionPlan candidate) {
        var key = candidate.name().toLowerCase(Locale.ROOT);
        return plans.stream()
                .filter(existing -> existing.id() != candidate.id())
                .map(NutritionPlan::name)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(key::equals);
    }
}
