package ru.npi.kbju;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Демонстрирует расчёт просроченных дат пересмотра целей КБЖУ.
 */
public final class Practice02App {
    private Practice02App() {
    }

    /**
     * Запускает воспроизводимый пример с датами до, на и после границы.
     *
     * @param args аргументы командной строки; в этой практике не используются
     */
    public static void main(String[] args) {
        LocalDate controlDate = LocalDate.of(2026, 9, 9);
        String[] goalNames = {
                "Снижение массы",
                "Поддержание массы",
                "Набор массы"
        };
        LocalDate[] plannedReviewDates = {
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 9),
                LocalDate.of(2026, 9, 10)
        };

        System.out.println("Контрольная дата: " + controlDate);
        System.out.println("Просроченные пересмотры целей:");

        for (int index = 0; index < plannedReviewDates.length; index++) {
            if (ReviewDeadlineCalculator.isOverdue(plannedReviewDates[index], controlDate)) {
                System.out.printf("- %s — %s%n", goalNames[index], plannedReviewDates[index]);
            }
        }

        int overdueCount = ReviewDeadlineCalculator.countOverdue(plannedReviewDates, controlDate);
        double overduePercentage = ReviewDeadlineCalculator.overduePercentage(plannedReviewDates, controlDate);

        System.out.printf("Итого просрочено: %d из %d%n", overdueCount, plannedReviewDates.length);
        System.out.printf(Locale.ROOT, "Доля просроченных: %.2f%%%n", overduePercentage);
    }
}
