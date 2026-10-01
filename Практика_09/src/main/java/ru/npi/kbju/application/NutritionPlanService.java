package ru.npi.kbju.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.domain.NutritionPlanRepository;

/** Прикладные сценарии работы с планами питания без зависимостей от JavaFX и памяти. */
public final class NutritionPlanService {
    private final NutritionPlanRepository repository;
    private final Clock clock;

    /** Создаёт сервис с внедрённым портом и системным источником времени. */
    public NutritionPlanService(NutritionPlanRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    /** Создаёт сервис с внедрёнными портом и заменяемым источником времени. */
    public NutritionPlanService(NutritionPlanRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Репозиторий обязателен");
        this.clock = Objects.requireNonNull(clock, "Источник времени обязателен");
    }

    /** Возвращает снимок планов через порт хранилища. */
    public List<NutritionPlan> all() {
        return repository.findAll();
    }

    /** Проверяет модель и сохраняет новый план через порт. */
    public NutritionPlan addPlan(String name, LocalDate effectiveFrom,
                                 NutritionPlan.Status status)
            throws DuplicatePlanException {
        var candidate = new NutritionPlan(0, name, effectiveFrom, status);
        return repository.save(candidate);
    }

    /** Добавляет черновик с датой внедрённого источника времени. */
    public NutritionPlan addDraft(String name) throws DuplicatePlanException {
        return addPlan(name, LocalDate.now(clock), NutritionPlan.Status.DRAFT);
    }

    /** Удаляет план по устойчивому идентификатору. */
    public boolean remove(long id) {
        return repository.deleteById(id);
    }

    /** Меняет состояние найденного по ID плана и сохраняет замену через порт. */
    public NutritionPlan changeStatus(long id, NutritionPlan.Status newStatus) {
        Objects.requireNonNull(newStatus, "Новое состояние обязательно");
        var current = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "План с ID " + id + " не найден"));
        try {
            return repository.save(current.withStatus(newStatus));
        } catch (DuplicatePlanException exception) {
            throw new IllegalStateException(
                    "Хранилище отклонило изменение существующего плана", exception);
        }
    }
}
