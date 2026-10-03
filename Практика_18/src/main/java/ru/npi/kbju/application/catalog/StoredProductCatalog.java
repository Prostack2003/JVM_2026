package ru.npi.kbju.application.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import ru.npi.kbju.domain.FoodProduct;

/** Полный подтверждённый каталог и время его получения. */
public record StoredProductCatalog(List<FoodProduct> products, Instant receivedAt) {
    /** Защищает снимок от изменения после публикации. */
    public StoredProductCatalog {
        products = List.copyOf(Objects.requireNonNull(products, "Каталог обязателен"));
        receivedAt = Objects.requireNonNull(receivedAt, "Время получения обязательно");
    }
}
