package ru.npi.kbju.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Типобезопасно считает повторения ключей.
 * {@code null} не допускается; экземпляр не является потокобезопасным.
 *
 * @param <T> тип учитываемого ключа
 */
public final class CountMap<T> {
    private final Map<T, Integer> counts = new HashMap<>();

    /** Создаёт пустой счётчик. */
    public CountMap() {
    }

    /**
     * Добавляет одно вхождение ключа.
     *
     * @param value ключ
     * @throws NullPointerException если ключ отсутствует
     * @throws ArithmeticException если целочисленный счётчик переполнен
     */
    public void add(T value) {
        T requiredValue = Objects.requireNonNull(value, "Ключ обязателен");
        counts.merge(requiredValue, 1, Math::addExact);
    }

    /**
     * Возвращает число вхождений ключа или ноль, если ключ не встречался.
     *
     * @param value ключ
     * @return число вхождений
     * @throws NullPointerException если ключ отсутствует
     */
    public int getCount(T value) {
        T requiredValue = Objects.requireNonNull(value, "Ключ обязателен");
        return counts.getOrDefault(requiredValue, 0);
    }

    /**
     * Удаляет ключ целиком и возвращает его количество до удаления.
     *
     * @param value ключ
     * @return прежнее число вхождений или ноль для неизвестного ключа
     * @throws NullPointerException если ключ отсутствует
     */
    public int remove(T value) {
        T requiredValue = Objects.requireNonNull(value, "Ключ обязателен");
        Integer previousCount = counts.remove(requiredValue);
        return previousCount == null ? 0 : previousCount;
    }

    /**
     * Возвращает число различных ключей.
     *
     * @return число ключей, а не сумма всех вхождений
     */
    public int size() {
        return counts.size();
    }

    /**
     * Прибавляет все количества из другого счётчика, не изменяя источник.
     * Источник производит значения типа {@code T}, поэтому принимается
     * {@code CountMap<? extends T>}.
     *
     * @param source источник количеств
     * @throws NullPointerException если источник отсутствует
     * @throws ArithmeticException если сумма количеств переполнена
     */
    public void addAll(CountMap<? extends T> source) {
        CountMap<? extends T> requiredSource = Objects.requireNonNull(source, "Источник обязателен");
        for (var entry : requiredSource.toMap().entrySet()) {
            counts.merge(entry.getKey(), entry.getValue(), Math::addExact);
        }
    }

    /**
     * Возвращает отдельный неизменяемый снимок текущих количеств.
     * Последующие изменения счётчика не изменяют ранее полученный снимок.
     *
     * @return неизменяемая копия отображения
     */
    public Map<T, Integer> toMap() {
        return Map.copyOf(counts);
    }

    /**
     * Копирует количества в карту, способную принимать ключи типа {@code T}.
     * Уже существующее значение того же ключа заменяется количеством из счётчика.
     *
     * @param destination карта-приёмник
     * @throws NullPointerException если приёмник отсутствует
     */
    public void copyTo(Map<? super T, Integer> destination) {
        Map<? super T, Integer> requiredDestination =
                Objects.requireNonNull(destination, "Приёмник обязателен");
        requiredDestination.putAll(counts);
    }
}
