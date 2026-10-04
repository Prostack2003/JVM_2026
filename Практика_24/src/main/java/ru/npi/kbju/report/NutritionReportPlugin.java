package ru.npi.kbju.report;

/** Публичный контракт доверенного расширения форматирования отчёта КБЖУ. */
public interface NutritionReportPlugin {
    /** Возвращает устойчивый идентификатор формата. */
    String id();

    /** Форматирует уже проверенную суточную сводку без ввода-вывода. */
    String format(DailyNutritionSummary summary);
}
