package ru.npi.kbju.application.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import ru.npi.kbju.domain.FoodProduct;

/** Атомарно заменяет подтверждённый каталог только после полной загрузки. */
public final class ProductCatalogRefreshService {
    private final ProductDirectory directory;
    private final AtomicReference<List<FoodProduct>> confirmed =
            new AtomicReference<>(List.of());

    /** Принимает порт внешнего справочника. */
    public ProductCatalogRefreshService(ProductDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "Справочник продуктов обязателен");
    }

    /**
     * Запускает синхронный HTTP-адаптер в переданном executor.
     * Ошибка любой записи оставляет прежний подтверждённый снимок.
     */
    public CompletableFuture<List<FoodProduct>> refreshAsync(Executor executor) {
        Objects.requireNonNull(executor, "Executor обязателен");
        return CompletableFuture.supplyAsync(() -> {
            var candidate = validate(directory.fetch());
            confirmed.set(candidate);
            return candidate;
        }, executor);
    }

    /** Возвращает последний полностью подтверждённый снимок. */
    public List<FoodProduct> confirmedProducts() {
        return confirmed.get();
    }

    private static List<FoodProduct> validate(List<FoodProduct> products) {
        var snapshot = List.copyOf(Objects.requireNonNull(products, "Каталог обязателен"));
        var codes = new HashSet<String>();
        for (var product : snapshot) {
            Objects.requireNonNull(product, "Каталог не должен содержать null");
            if (!codes.add(product.code())) {
                throw new DirectoryLoadException(
                        DirectoryLoadException.Kind.CONTRACT,
                        "В каталоге повторяется код " + product.code());
            }
        }
        return snapshot;
    }
}
