package ru.npi.kbju.application.observation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Objects;
import ru.npi.kbju.application.NutritionPlanOperations;

/** Создаёт proxy, который наблюдает контракт, не меняя результаты и исключения сервиса. */
public final class ObservedNutritionPlanService {
    private ObservedNutritionPlanService() {
    }

    /** Оборачивает доверенную реализацию интерфейса и считает отмеченные операции. */
    public static NutritionPlanOperations create(
            NutritionPlanOperations target,
            OperationCallMetrics metrics
    ) {
        Objects.requireNonNull(target, "Целевой сервис обязателен");
        Objects.requireNonNull(metrics, "Счётчик вызовов обязателен");

        return (NutritionPlanOperations) Proxy.newProxyInstance(
                NutritionPlanOperations.class.getClassLoader(),
                new Class<?>[]{NutritionPlanOperations.class},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> proxy == arguments[0];
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "toString" -> "Наблюдаемый сервис планов питания";
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if (method.isAnnotationPresent(OperationTitle.class)) {
                        metrics.record(method.getName());
                    }
                    try {
                        return method.invoke(target, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }
}
