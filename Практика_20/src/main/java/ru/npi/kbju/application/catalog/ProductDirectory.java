package ru.npi.kbju.application.catalog;

import java.util.List;
import ru.npi.kbju.domain.FoodProduct;

/** Прикладной порт к внешнему справочнику без URL, HTTP и JSON. */
@FunctionalInterface
public interface ProductDirectory {
    /** Возвращает полный проверенный снимок внешнего каталога. */
    List<FoodProduct> fetch();
}
