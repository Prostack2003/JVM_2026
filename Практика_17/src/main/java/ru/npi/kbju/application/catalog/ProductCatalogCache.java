package ru.npi.kbju.application.catalog;

/** Порт подтверждённого кэша без знания о файлах и JSON. */
public interface ProductCatalogCache {
    /** Читает целый подтверждённый снимок. */
    StoredProductCatalog read();

    /** Заменяет кэш только целым проверенным снимком. */
    void write(StoredProductCatalog catalog);
}
