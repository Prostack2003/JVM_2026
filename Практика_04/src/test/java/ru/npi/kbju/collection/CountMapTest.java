package ru.npi.kbju.collection;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Проверяет контракт универсального счётчика повторений. */
class CountMapTest {
    /** Проверяет пустой набор и неизвестный ключ. */
    @Test
    @DisplayName("Пустой счётчик возвращает ноль")
    void emptyCounterReturnsZero() {
        var counter = new CountMap<String>();

        assertEquals(0, counter.size());
        assertEquals(0, counter.getCount("FOOD-404"));
        assertTrue(counter.toMap().isEmpty());
    }

    /** Проверяет сохранение повторов, которые потерял бы Set. */
    @Test
    @DisplayName("Повторные коды увеличивают кратность")
    void repeatedCodesIncreaseCount() {
        var counter = new CountMap<String>();

        counter.add("FOOD-101");
        counter.add("FOOD-101");
        counter.add("FOOD-102");

        assertEquals(2, counter.size());
        assertEquals(2, counter.getCount("FOOD-101"));
        assertEquals(1, counter.getCount("FOOD-102"));
    }

    /** Проверяет прежнее значение, которое должен возвращать remove. */
    @Test
    @DisplayName("Удаление возвращает прежнюю кратность")
    void removeReturnsPreviousCount() {
        var counter = new CountMap<String>();
        counter.add("FOOD-101");
        counter.add("FOOD-101");

        assertEquals(2, counter.remove("FOOD-101"));
        assertEquals(0, counter.getCount("FOOD-101"));
        assertEquals(0, counter.remove("FOOD-404"));
    }

    /** Проверяет суммирование и неизменность исходного второго счётчика. */
    @Test
    @DisplayName("Объединение суммирует повторы и сохраняет источник")
    void addAllSumsCountsAndPreservesSource() {
        var target = counterOf("FOOD-101", "FOOD-101", "FOOD-102");
        var source = counterOf("FOOD-101", "FOOD-103", "FOOD-103");
        var sourceBefore = source.toMap();

        target.addAll(source);

        assertEquals(Map.of("FOOD-101", 3, "FOOD-102", 1, "FOOD-103", 2), target.toMap());
        assertEquals(sourceBefore, source.toMap());
    }

    /** Проверяет безопасное самообъединение через снимок источника. */
    @Test
    @DisplayName("Самообъединение удваивает значения")
    void addAllSupportsSameInstance() {
        var counter = counterOf("FOOD-101", "FOOD-101", "FOOD-102");

        counter.addAll(counter);

        assertEquals(4, counter.getCount("FOOD-101"));
        assertEquals(2, counter.getCount("FOOD-102"));
    }

    /** Проверяет запрет изменения результата toMap. */
    @Test
    @DisplayName("Снимок нельзя изменить")
    void snapshotIsUnmodifiable() {
        var counter = counterOf("FOOD-101");
        var snapshot = counter.toMap();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("FOOD-999", 1));
        assertEquals(Map.of("FOOD-101", 1), counter.toMap());
    }

    /** Проверяет независимость ранее полученного снимка. */
    @Test
    @DisplayName("Снимок не меняется вслед за счётчиком")
    void snapshotIsDetachedFromLaterChanges() {
        var counter = counterOf("FOOD-101");
        var snapshot = counter.toMap();

        counter.add("FOOD-102");

        assertFalse(snapshot.containsKey("FOOD-102"));
        assertEquals(Map.of("FOOD-101", 1), snapshot);
    }

    /** Проверяет нижнюю границу типа и замену существующего значения. */
    @Test
    @DisplayName("copyTo записывает в карту надтипа")
    void copyToWritesToSupertypeMapAndReplacesValue() {
        var counter = counterOf("FOOD-101", "FOOD-101");
        Map<CharSequence, Integer> destination = new HashMap<>();
        destination.put("FOOD-101", 99);

        counter.copyTo(destination);

        assertEquals(2, destination.get("FOOD-101"));
    }

    /** Проверяет явную сортировку результата вместо зависимости от HashMap. */
    @Test
    @DisplayName("TreeMap сортирует результат по коду")
    void treeMapSortsByProductCode() {
        var counter = counterOf("FOOD-103", "FOOD-101", "FOOD-102");

        var sorted = new TreeMap<>(counter.toMap());

        assertEquals(List.of("FOOD-101", "FOOD-102", "FOOD-103"),
                List.copyOf(sorted.keySet()));
    }

    /** Проверяет отказ для null во всех операциях с ключом. */
    @Test
    @DisplayName("Отсутствующий ключ отклоняется")
    void nullKeyIsRejected() {
        var counter = new CountMap<String>();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> counter.add(null)),
                () -> assertThrows(NullPointerException.class, () -> counter.getCount(null)),
                () -> assertThrows(NullPointerException.class, () -> counter.remove(null))
        );
    }

    /** Проверяет отказ для отсутствующих источника и приёмника. */
    @Test
    @DisplayName("Отсутствующие источник и приёмник отклоняются")
    void nullCollaboratorsAreRejected() {
        var counter = new CountMap<String>();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> counter.addAll(null)),
                () -> assertThrows(NullPointerException.class, () -> counter.copyTo(null))
        );
    }

    /** Подтверждает, что универсальный класс принимает другой тип ключа. */
    @Test
    @DisplayName("Обобщённый счётчик поддерживает Integer")
    void genericCounterSupportsIntegerKeys() {
        var counter = new CountMap<Integer>();
        counter.add(10);
        counter.add(10);

        assertEquals(2, counter.getCount(10));
    }

    private static CountMap<String> counterOf(String... values) {
        var counter = new CountMap<String>();
        for (String value : values) {
            counter.add(value);
        }
        return counter;
    }
}
