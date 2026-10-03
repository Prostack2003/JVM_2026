package ru.npi.kbju.integration.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.domain.FoodProduct;

/** Проверяет внешний JSON-контракт до создания предметных записей. */
public final class FoodProductJsonMapper {
    private static final int CONTRACT_VERSION = 1;
    private static final int MAX_PRODUCTS = 1_000;
    private final ObjectMapper mapper;

    /** Создаёт маппер с явным проходом DTO-границы. */
    public FoodProductJsonMapper() {
        mapper = new ObjectMapper();
    }

    /** Возвращает полный неизменяемый каталог или точную ошибку JSON/контракта. */
    public List<FoodProduct> parse(String json) {
        Objects.requireNonNull(json, "JSON обязателен");
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.MALFORMED_JSON,
                    "Сервер вернул повреждённый JSON", exception);
        }

        require(root != null && root.isObject(), "Корень JSON должен быть объектом");
        var version = root.get("version");
        require(version != null && version.isIntegralNumber()
                        && version.canConvertToInt()
                        && version.intValue() == CONTRACT_VERSION,
                "version должен быть целым числом 1");

        var productsNode = root.get("products");
        require(productsNode != null && productsNode.isArray(),
                "products должен быть массивом");
        require(productsNode.size() <= MAX_PRODUCTS,
                "Каталог не должен содержать больше " + MAX_PRODUCTS + " записей");

        var result = new ArrayList<FoodProduct>(productsNode.size());
        var codes = new HashSet<String>();
        for (var index = 0; index < productsNode.size(); index++) {
            var node = productsNode.get(index);
            require(node.isObject(), field(index, "запись") + " должна быть объектом");
            var product = toDomain(node, index);
            require(codes.add(product.code()),
                    "В JSON повторяется код " + product.code());
            result.add(product);
        }
        return List.copyOf(result);
    }

    private static FoodProduct toDomain(JsonNode node, int index) {
        try {
            return new FoodProduct(
                    requiredText(node, "code", index),
                    requiredText(node, "name", index),
                    requiredInt(node, "energyKcal", index),
                    requiredDecimal(node, "proteinGrams", index),
                    requiredDecimal(node, "fatGrams", index),
                    requiredDecimal(node, "carbohydrateGrams", index));
        } catch (IllegalArgumentException exception) {
            throw new DirectoryLoadException(
                    DirectoryLoadException.Kind.CONTRACT,
                    "Недопустимый продукт в products[" + index + "]: " + exception.getMessage(),
                    exception);
        }
    }

    private static String requiredText(JsonNode node, String name, int index) {
        var value = node.get(name);
        require(value != null && value.isTextual(), field(index, name) + " должно быть строкой");
        var text = value.textValue().strip();
        require(!text.isEmpty(), field(index, name) + " не должно быть пустым");
        return text;
    }

    private static int requiredInt(JsonNode node, String name, int index) {
        var value = node.get(name);
        require(value != null && value.isIntegralNumber() && value.canConvertToInt(),
                field(index, name) + " должно быть целым числом");
        return value.intValue();
    }

    private static BigDecimal requiredDecimal(JsonNode node, String name, int index) {
        var value = node.get(name);
        require(value != null && value.isNumber(),
                field(index, name) + " должно быть числом");
        return value.decimalValue();
    }

    private static String field(int index, String name) {
        return "products[" + index + "]." + name;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new DirectoryLoadException(DirectoryLoadException.Kind.CONTRACT, message);
        }
    }
}
