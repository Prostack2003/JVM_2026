package ru.npi.kbju.application.observation;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Читает только метаданные публичного контракта и не выполняет его методы. */
public final class OperationCatalog {
    private OperationCatalog() {
    }

    /** Возвращает отсортированные русские описания аннотированных public-методов. */
    public static List<OperationDescriptor> describe(Class<?> serviceContract) {
        Objects.requireNonNull(serviceContract, "Контракт сервиса обязателен");
        return Arrays.stream(serviceContract.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(OperationTitle.class))
                .map(method -> new OperationDescriptor(
                        method.getName(), method.getAnnotation(OperationTitle.class).value()))
                .sorted(Comparator.comparing(OperationDescriptor::methodName))
                .toList();
    }
}
