package ru.npi.kbju.application.observation;

import java.util.Objects;

/** Безопасное описание аннотированной операции без возможности её вызвать. */
public record OperationDescriptor(String methodName, String title) {
    /** Проверяет обязательность обеих частей описания. */
    public OperationDescriptor {
        Objects.requireNonNull(methodName, "Имя метода обязательно");
        Objects.requireNonNull(title, "Название операции обязательно");
    }
}
