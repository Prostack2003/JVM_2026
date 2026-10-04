package ru.npi.kbju.application.concurrency;

import java.util.List;

/** Полный итог параллельной диагностики после завершения всех задач. */
public record DiagnosticBatch(
        List<PlanDiagnostic> diagnostics,
        int submittedTasks,
        int completedTasks,
        int peakConcurrency,
        boolean executorTerminated
) {
    /** Фиксирует неизменяемый снимок и согласованные счётчики. */
    public DiagnosticBatch {
        diagnostics = List.copyOf(diagnostics);
        if (submittedTasks < 0 || completedTasks < 0 || peakConcurrency < 0) {
            throw new IllegalArgumentException("Счётчики пакета не могут быть отрицательными");
        }
        if (completedTasks != diagnostics.size() || submittedTasks < completedTasks) {
            throw new IllegalArgumentException("Итоги не согласованы со счётчиками задач");
        }
    }
}
