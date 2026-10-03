package ru.npi.kbju.application.concurrency;

import java.util.Objects;

/** Неизменяемый результат чтения одного плана в фоновой задаче. */
public record PlanDiagnostic(
        long planId,
        String planName,
        boolean requiresAttention,
        String summary,
        boolean virtualThread
) {
    /** Проверяет, что результат можно безопасно передать между потоками. */
    public PlanDiagnostic {
        if (planId <= 0) {
            throw new IllegalArgumentException("Диагностике нужен устойчивый ID");
        }
        planName = requireText(planName, "Название плана обязательно");
        summary = requireText(summary, "Итог диагностики обязателен");
    }

    private static String requireText(String value, String message) {
        var text = Objects.requireNonNull(value, message).strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }
}
