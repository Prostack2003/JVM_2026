package ru.npi.kbju.integration.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Objects;
import ru.npi.kbju.application.catalog.CatalogCacheException;
import ru.npi.kbju.application.catalog.DirectoryLoadException;
import ru.npi.kbju.application.catalog.ProductCatalogCache;
import ru.npi.kbju.application.catalog.StoredProductCatalog;
import ru.npi.kbju.integration.http.FoodProductJsonMapper;

/** JSON-кэш, который заменяется только после полной записи временного файла. */
public final class FileProductCatalogCache implements ProductCatalogCache {
    private static final int CACHE_VERSION = 1;

    private final Path cacheFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final FoodProductJsonMapper productMapper = new FoodProductJsonMapper();

    /** Создаёт адаптер для одного файла кэша. */
    public FileProductCatalogCache(Path cacheFile) {
        this.cacheFile = Objects.requireNonNull(cacheFile, "Путь кэша обязателен").toAbsolutePath();
    }

    /** Читает и полностью перепроверяет JSON-кэш. */
    @Override
    public StoredProductCatalog read() {
        if (!Files.exists(cacheFile)) {
            throw new CatalogCacheException(
                    CatalogCacheException.Kind.NO_SAVED_DATA,
                    "Файл кэша ещё не создан");
        }
        final String json;
        try {
            json = Files.readString(cacheFile, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new CatalogCacheException(
                    CatalogCacheException.Kind.READ_FAILED,
                    "Не удалось прочитать файл кэша",
                    failure);
        }

        try {
            JsonNode root = mapper.readTree(json);
            require(root != null && root.isObject(), "Корень кэша должен быть объектом");
            var cacheVersion = root.get("cacheVersion");
            require(cacheVersion != null && cacheVersion.isInt()
                            && cacheVersion.intValue() == CACHE_VERSION,
                    "cacheVersion должен быть целым числом 1");
            var receivedAtNode = root.get("receivedAt");
            require(receivedAtNode != null && receivedAtNode.isTextual(),
                    "receivedAt должен быть ISO-8601 строкой");
            var receivedAt = Instant.parse(receivedAtNode.textValue());
            var products = productMapper.parse(json);
            return new StoredProductCatalog(products, receivedAt);
        } catch (JsonProcessingException | DateTimeException | DirectoryLoadException failure) {
            throw corrupt(failure.getMessage(), failure);
        } catch (CatalogCacheException failure) {
            throw failure;
        }
    }

    /** Пишет новую копию рядом и затем заменяет прежний кэш. */
    @Override
    public void write(StoredProductCatalog catalog) {
        Objects.requireNonNull(catalog, "Каталог для кэша обязателен");
        Path temporary = null;
        try {
            var parent = cacheFile.getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".kbju-catalog-", ".tmp");
            Files.writeString(temporary, toJson(catalog), StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        cacheFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new CatalogCacheException(
                    CatalogCacheException.Kind.WRITE_FAILED,
                    "Не удалось атомарно обновить кэш",
                    failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Ошибка уборки не подменяет исходный отказ записи.
                }
            }
        }
    }

    /** Возвращает фактический файл для диагностики и демонстрации. */
    public Path cacheFile() {
        return cacheFile;
    }

    private String toJson(StoredProductCatalog catalog) throws JsonProcessingException {
        var root = mapper.createObjectNode();
        root.put("cacheVersion", CACHE_VERSION);
        root.put("receivedAt", catalog.receivedAt().toString());
        root.put("version", 1);
        var products = root.putArray("products");
        for (var product : catalog.products()) {
            var node = products.addObject();
            node.put("code", product.code());
            node.put("name", product.name());
            node.put("energyKcal", product.energyKcal());
            node.put("proteinGrams", product.proteinGrams());
            node.put("fatGrams", product.fatGrams());
            node.put("carbohydrateGrams", product.carbohydrateGrams());
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw corrupt(message, null);
        }
    }

    private static CatalogCacheException corrupt(String message, Throwable cause) {
        return new CatalogCacheException(
                CatalogCacheException.Kind.CORRUPT_CACHE,
                "Повреждённый кэш: " + message,
                cause);
    }
}
