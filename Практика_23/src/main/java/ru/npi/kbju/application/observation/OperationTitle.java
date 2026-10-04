package ru.npi.kbju.application.observation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Русское описание публичной прикладной операции, доступное во время выполнения. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperationTitle {
    /** Возвращает понятное пользователю или оператору название операции. */
    String value();
}
