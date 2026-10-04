package ru.npi.kbju.application.audit;

/** Неизменяемое сообщение о завершённой порции фоновой работы. */
public record AuditProgress(int completed, int total, String checkName) {
    /** Защищает контракт прогресса. */
    public AuditProgress {
        if (total < 0 || completed < 0 || completed > total) {
            throw new IllegalArgumentException("Недопустимые границы прогресса");
        }
        if (checkName == null || checkName.isBlank()) {
            throw new IllegalArgumentException("Название этапа проверки обязательно");
        }
    }
}
