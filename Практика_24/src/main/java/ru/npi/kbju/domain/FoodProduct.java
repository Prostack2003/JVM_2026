package ru.npi.kbju.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/** Проверенная предметная запись справочника продуктов на 100 граммов. */
public record FoodProduct(
        String code,
        String name,
        int energyKcal,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        BigDecimal carbohydrateGrams
) {
    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9_-]{2,19}");
    private static final int MAX_NAME_LENGTH = 80;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** Защищает модель от недопустимых внешних данных. */
    public FoodProduct {
        code = requireText(code, "Код продукта обязателен");
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Код продукта должен состоять из 3–20 латинских букв, цифр, _ и -");
        }
        name = requireText(name, "Название продукта обязательно");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Название продукта не должно быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        if (energyKcal < 0 || energyKcal > 1_000) {
            throw new IllegalArgumentException("Энергетическая ценность должна быть от 0 до 1000 ккал");
        }
        proteinGrams = requireNutrient(proteinGrams, "Белки");
        fatGrams = requireNutrient(fatGrams, "Жиры");
        carbohydrateGrams = requireNutrient(carbohydrateGrams, "Углеводы");
    }

    private static String requireText(String value, String message) {
        var result = Objects.requireNonNull(value, message).strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private static BigDecimal requireNutrient(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " обязательны");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(field + " должны быть от 0 до 100 г");
        }
        return value.stripTrailingZeros();
    }
}
