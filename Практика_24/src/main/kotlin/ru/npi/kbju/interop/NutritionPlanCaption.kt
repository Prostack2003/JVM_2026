package ru.npi.kbju.interop

import ru.npi.kbju.domain.NutritionPlan

/**
 * Изолированный Kotlin-форматировщик Java-модели без доступа к UI и хранилищу.
 * Nullable-вход выбран намеренно: Java может передать null, поэтому граница
 * отвергает его с устойчивым предметным сообщением вместо неявного platform type.
 */
object NutritionPlanCaption {
    /** Формирует стабильную подпись плана для Java-клиента. */
    @JvmStatic
    fun format(plan: NutritionPlan?): String {
        val requiredPlan = requireNotNull(plan) { "План питания обязателен" }
        return "План #${requiredPlan.id}: ${requiredPlan.name} " +
            "[${requiredPlan.status.name}], действует с ${requiredPlan.effectiveFrom}"
    }
}
