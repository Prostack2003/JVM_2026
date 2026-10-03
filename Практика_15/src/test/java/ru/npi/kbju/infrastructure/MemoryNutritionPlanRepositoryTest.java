package ru.npi.kbju.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;

/** Проверяет контракт адаптера памяти независимо от JavaFX. */
class MemoryNutritionPlanRepositoryTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

    @Test
    void repositoriesHaveIndependentState() throws DuplicatePlanException {
        var first = new MemoryNutritionPlanRepository();
        var second = new MemoryNutritionPlanRepository();

        first.save(candidate("Поддержание массы"));

        assertEquals(1, first.findAll().size());
        assertTrue(second.findAll().isEmpty());
    }

    @Test
    void duplicateFailureIsAtomicAndDoesNotConsumeIdentifier()
            throws DuplicatePlanException {
        var repository = new MemoryNutritionPlanRepository();
        var first = repository.save(candidate("Снижение массы"));
        var beforeFailure = repository.findAll();

        assertThrows(DuplicatePlanException.class,
                () -> repository.save(candidate(" снижение МАССЫ ")));

        assertEquals(beforeFailure, repository.findAll());
        var next = repository.save(candidate("Поддержание массы"));
        assertEquals(first.id() + 1, next.id());
    }

    @Test
    void updateAndDeleteUseStableIdentifier() throws DuplicatePlanException {
        var repository = new MemoryNutritionPlanRepository();
        var saved = repository.save(candidate("Снижение массы"));
        var replacement = repository.save(saved.withStatus(NutritionPlan.Status.ACTIVE));

        assertEquals(saved.id(), replacement.id());
        assertEquals(NutritionPlan.Status.ACTIVE,
                repository.findById(saved.id()).orElseThrow().status());
        assertTrue(repository.deleteById(saved.id()));
        assertFalse(repository.deleteById(saved.id()));
    }

    private static NutritionPlan candidate(String name) {
        return new NutritionPlan(0, name, DATE, NutritionPlan.Status.DRAFT);
    }
}
