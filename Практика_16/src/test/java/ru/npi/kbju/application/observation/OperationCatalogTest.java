package ru.npi.kbju.application.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.application.NutritionPlanOperations;

/** Проверяет доступность русских метаданных во время выполнения. */
class OperationCatalogTest {
    @Test
    void findsOnlyAnnotatedPublicOperationsInStableOrder() {
        var descriptions = OperationCatalog.describe(NutritionPlanOperations.class);

        assertEquals(List.of(
                new OperationDescriptor("addPlan", "Создать план питания"),
                new OperationDescriptor("changeStatus", "Изменить состояние плана питания")
        ), descriptions);
        assertFalse(descriptions.stream()
                .anyMatch(operation -> operation.methodName().equals("all")));
    }

    @Test
    void annotationIsRetainedAtRuntimeAndTargetsOnlyMethods() {
        var retention = OperationTitle.class.getAnnotation(Retention.class);
        var target = OperationTitle.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertEquals(List.of(ElementType.METHOD), List.of(target.value()));
        assertTrue(List.of(NutritionPlanOperations.class.getMethods()).stream()
                .allMatch(method -> Modifier.isPublic(method.getModifiers())));
    }
}
