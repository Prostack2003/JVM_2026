package ru.npi.kbju;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import ru.npi.kbju.collection.CountMap;

/** Демонстрирует подсчёт обращений к карточкам продуктов по предметному коду. */
public final class Practice04App {
    private Practice04App() {
    }

    /**
     * Выполняет воспроизводимый сценарий повторов, объединения и защищённого снимка.
     *
     * @param args аргументы командной строки; не используются
     */
    public static void main(String[] args) {
        var productAccesses = new CountMap<String>();
        productAccesses.add("FOOD-101");
        productAccesses.add("FOOD-101");
        productAccesses.add("FOOD-102");

        var initialSnapshot = productAccesses.toMap();

        var additionalAccesses = new CountMap<String>();
        additionalAccesses.add("FOOD-101");
        additionalAccesses.add("FOOD-103");
        additionalAccesses.add("FOOD-103");

        productAccesses.addAll(additionalAccesses);
        var mergedSnapshot = productAccesses.toMap();

        var sortedMergedSnapshot = new TreeMap<>(mergedSnapshot);

        System.out.println("Первый набор: " + new TreeMap<>(initialSnapshot));
        System.out.println("После объединения: " + sortedMergedSnapshot);
        System.out.println("Исходное отображение содержит кодов: " + mergedSnapshot.size());
        System.out.println("Содержимое после сортировки совпадает: "
                + sortedMergedSnapshot.equals(mergedSnapshot));
        System.out.println("Источник объединения сохранён: "
                + additionalAccesses.toMap().equals(Map.of("FOOD-101", 1, "FOOD-103", 2)));
        System.out.println("Изменение снимка запрещено: " + isMutationRejected(mergedSnapshot));

        productAccesses.add("FOOD-104");
        System.out.println("Ранее полученный снимок не изменился: "
                + !mergedSnapshot.containsKey("FOOD-104"));
        System.out.println("Отсортированный итог: " + new TreeMap<>(productAccesses.toMap()));

        Map<CharSequence, Integer> destination = new HashMap<>();
        destination.put("FOOD-101", 99);
        productAccesses.copyTo(destination);
        System.out.println("Копия в карту надтипа: " + new TreeMap<>(destination));
    }

    private static boolean isMutationRejected(Map<String, Integer> snapshot) {
        try {
            snapshot.put("FOOD-999", 1);
            return false;
        } catch (UnsupportedOperationException expected) {
            return true;
        }
    }
}
