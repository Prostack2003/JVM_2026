package ru.npi.kbju.application.audit;

/** Подтверждённый итог полного аудита неизменяемого снимка планов. */
public record AuditResult(
        int checkedPlans,
        int activePlans,
        int plansNeedingReview,
        int completedChecks
) {
    /** Защищает итог от противоречивых счётчиков. */
    public AuditResult {
        if (checkedPlans < 0 || activePlans < 0 || plansNeedingReview < 0
                || completedChecks < 0) {
            throw new IllegalArgumentException("Счётчики аудита не могут быть отрицательными");
        }
        if (activePlans + plansNeedingReview > checkedPlans) {
            throw new IllegalArgumentException("Счётчики состояний превышают число планов");
        }
    }
}
