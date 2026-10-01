package ru.npi.kbju;

import java.time.LocalDate;

/**
 * Проверяет нормальные, граничные и некорректные входные данные без внешних библиотек.
 */
public final class Practice02Checks {
    private static final LocalDate CONTROL_DATE = LocalDate.of(2026, 9, 9);
    private static int passedChecks;

    private Practice02Checks() {
    }

    /**
     * Выполняет все проверки и завершает процесс с ошибкой при нарушении контракта.
     *
     * @param args аргументы командной строки; не используются
     */
    public static void main(String[] args) {
        checkInt(
                "Три даты вокруг границы",
                1,
                ReviewDeadlineCalculator.countOverdue(new LocalDate[]{
                        CONTROL_DATE.minusDays(1), CONTROL_DATE, CONTROL_DATE.plusDays(1)
                }, CONTROL_DATE)
        );
        checkInt(
                "Пустой массив",
                0,
                ReviewDeadlineCalculator.countOverdue(new LocalDate[0], CONTROL_DATE)
        );
        checkBoolean(
                "Дата ровно в срок",
                false,
                ReviewDeadlineCalculator.isOverdue(CONTROL_DATE, CONTROL_DATE)
        );
        checkInt(
                "Дубликаты просроченной даты",
                2,
                ReviewDeadlineCalculator.countOverdue(new LocalDate[]{
                        CONTROL_DATE.minusDays(1), CONTROL_DATE.minusDays(1), CONTROL_DATE
                }, CONTROL_DATE)
        );
        checkInt(
                "Одна дата до границы",
                1,
                ReviewDeadlineCalculator.countOverdue(new LocalDate[]{CONTROL_DATE.minusDays(1)}, CONTROL_DATE)
        );
        checkInt(
                "Одна дата после границы",
                0,
                ReviewDeadlineCalculator.countOverdue(new LocalDate[]{CONTROL_DATE.plusDays(1)}, CONTROL_DATE)
        );
        checkDouble(
                "Процент для одной просроченной из трёх",
                (double) 1 / 3 * 100.0,
                ReviewDeadlineCalculator.overduePercentage(new LocalDate[]{
                        CONTROL_DATE.minusDays(1), CONTROL_DATE, CONTROL_DATE.plusDays(1)
                }, CONTROL_DATE)
        );
        checkDouble(
                "Процент для пустого массива",
                0.0,
                ReviewDeadlineCalculator.overduePercentage(new LocalDate[0], CONTROL_DATE)
        );
        checkDouble(
                "Процент для двух просроченных из четырёх",
                50.0,
                ReviewDeadlineCalculator.overduePercentage(new LocalDate[]{
                        CONTROL_DATE.minusDays(2), CONTROL_DATE.minusDays(1),
                        CONTROL_DATE, CONTROL_DATE.plusDays(1)
                }, CONTROL_DATE)
        );
        checkNullDateRejected();

        System.out.printf("Итог: %d из %d проверок пройдено.%n", passedChecks, 10);
    }

    private static void checkInt(String scenario, int expected, int actual) {
        printResult(scenario, Integer.toString(expected), Integer.toString(actual), expected == actual);
    }

    private static void checkBoolean(String scenario, boolean expected, boolean actual) {
        printResult(scenario, Boolean.toString(expected), Boolean.toString(actual), expected == actual);
    }

    private static void checkDouble(String scenario, double expected, double actual) {
        boolean equal = Math.abs(expected - actual) < 0.000_001;
        printResult(scenario, Double.toString(expected), Double.toString(actual), equal);
    }

    private static void checkNullDateRejected() {
        String scenario = "Отсутствующая дата отклоняется";
        try {
            ReviewDeadlineCalculator.countOverdue(new LocalDate[]{null}, CONTROL_DATE);
            printResult(scenario, "NullPointerException", "исключение не возникло", false);
        } catch (NullPointerException exception) {
            printResult(scenario, "NullPointerException", exception.getClass().getSimpleName(), true);
        }
    }

    private static void printResult(String scenario, String expected, String actual, boolean passed) {
        System.out.println("Сценарий: " + scenario);
        System.out.println("Ожидается: " + expected);
        System.out.println("Фактически: " + actual);
        System.out.println("Статус: " + (passed ? "ПРОЙДЕН" : "НЕ ПРОЙДЕН"));
        System.out.println();

        if (!passed) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        passedChecks++;
    }
}
