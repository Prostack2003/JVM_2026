package ru.npi.kbju.application.observation;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Потокобезопасно считает попытки вызова отмеченных предметных операций. */
public final class OperationCallMetrics {
    private final ConcurrentHashMap<String, LongAdder> calls = new ConcurrentHashMap<>();

    /** Создаёт пустой набор обезличенных счётчиков. */
    public OperationCallMetrics() {
    }

    /** Учитывает одну попытку выполнить операцию. */
    void record(String methodName) {
        Objects.requireNonNull(methodName, "Имя метода обязательно");
        calls.computeIfAbsent(methodName, ignored -> new LongAdder()).increment();
    }

    /** Возвращает число попыток конкретной операции. */
    public long count(String methodName) {
        var counter = calls.get(Objects.requireNonNull(methodName, "Имя метода обязательно"));
        return counter == null ? 0 : counter.sum();
    }

    /** Возвращает суммарное число вызовов отмеченных операций. */
    public long total() {
        return calls.values().stream().mapToLong(LongAdder::sum).sum();
    }

    /** Возвращает неизменяемый и стабильно отсортированный снимок счётчиков. */
    public Map<String, Long> snapshot() {
        var snapshot = new TreeMap<String, Long>();
        calls.forEach((name, counter) -> snapshot.put(name, counter.sum()));
        return Collections.unmodifiableMap(snapshot);
    }
}
