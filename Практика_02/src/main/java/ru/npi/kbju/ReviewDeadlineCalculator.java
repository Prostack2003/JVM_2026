package ru.npi.kbju;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Выполняет расчёты для плановых дат пересмотра целей питания.
 */
public final class ReviewDeadlineCalculator {
    private ReviewDeadlineCalculator() {
    }

    /**
     * Определяет, просрочен ли пересмотр цели на контрольную дату.
     * Дата, совпадающая с контрольной, ещё находится в сроке.
     *
     * @param plannedDate плановая дата пересмотра
     * @param controlDate контрольная дата
     * @return {@code true}, если плановая дата строго раньше контрольной
     */
    public static boolean isOverdue(LocalDate plannedDate, LocalDate controlDate) {
        Objects.requireNonNull(plannedDate, "Плановая дата обязательна");
        Objects.requireNonNull(controlDate, "Контрольная дата обязательна");
        return plannedDate.isBefore(controlDate);
    }

    /**
     * Считает все просроченные пересмотры, не изменяя исходный массив.
     * Повторяющиеся даты рассматриваются как отдельные записи.
     *
     * @param plannedDates плановые даты пересмотров
     * @param controlDate контрольная дата
     * @return количество просроченных записей
     */
    public static int countOverdue(LocalDate[] plannedDates, LocalDate controlDate) {
        Objects.requireNonNull(plannedDates, "Массив плановых дат обязателен");
        Objects.requireNonNull(controlDate, "Контрольная дата обязательна");

        int overdueCount = 0;
        for (int index = 0; index < plannedDates.length; index++) {
            if (isOverdue(plannedDates[index], controlDate)) {
                overdueCount++;
            }
        }
        return overdueCount;
    }

    /**
     * Вычисляет долю просроченных пересмотров в процентах.
     * Для пустого массива возвращает {@code 0.0} без деления на ноль.
     *
     * @param plannedDates плановые даты пересмотров
     * @param controlDate контрольная дата
     * @return процент просроченных записей от 0.0 до 100.0
     */
    public static double overduePercentage(LocalDate[] plannedDates, LocalDate controlDate) {
        Objects.requireNonNull(plannedDates, "Массив плановых дат обязателен");
        Objects.requireNonNull(controlDate, "Контрольная дата обязательна");

        if (plannedDates.length == 0) {
            return 0.0;
        }

        int overdueCount = countOverdue(plannedDates, controlDate);
        return (double) overdueCount / plannedDates.length * 100.0;
    }
}
