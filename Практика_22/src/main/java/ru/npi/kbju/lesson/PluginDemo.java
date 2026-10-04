package ru.npi.kbju.lesson;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ServiceLoader;
import ru.npi.kbju.report.DailyNutritionSummary;
import ru.npi.kbju.report.NutritionReportPlugin;

/** Загружает доверенные форматы отчёта из собранного JAR. */
public final class PluginDemo {
    private PluginDemo() {
    }

    /** Показывает два формата без знания классов их реализаций в клиентском цикле. */
    public static void main(String[] args) {
        var summary = new DailyNutritionSummary(
                LocalDate.of(2026, 10, 3), 1850, 2100, 115.0, 62.0, 210.0);
        var providers = ServiceLoader.load(NutritionReportPlugin.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(NutritionReportPlugin::id))
                .toList();
        if (providers.isEmpty()) {
            throw new IllegalStateException("Поставщики отчётов не найдены: проверьте META-INF/services");
        }

        var ids = new HashSet<String>();
        var reports = new HashSet<String>();
        System.out.println("Суточный отчёт КБЖУ загружен из поставщиков:");
        for (var provider : providers) {
            var id = provider.id();
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Поставщик вернул пустой идентификатор");
            }
            if (!ids.add(id)) {
                throw new IllegalStateException("Повторяется идентификатор поставщика: " + id);
            }
            var report = provider.format(summary);
            if (report == null || report.isBlank()) {
                throw new IllegalStateException("Поставщик вернул пустой отчёт: " + id);
            }
            reports.add(report);
            System.out.printf("[%s] %s%n", id, report);
        }
        if (reports.size() != providers.size()) {
            throw new IllegalStateException("Поставщики должны создавать разные форматы");
        }
        System.out.println("Поставщиков: " + providers.size());
        System.out.println("ServiceLoader обнаруживает доверенный код, но не создаёт песочницу.");
    }
}
