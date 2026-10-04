package ru.npi.kbju.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import ru.npi.kbju.domain.NutritionPlan;

/** Фиксирует JVM-сигнатуру, значения Java-модели и null-контракт Kotlin-кода. */
class KotlinInteropTest {
    @Test
    void javaCallsKotlinFormatterWhichReadsJavaRecord() {
        var plan = new NutritionPlan(
                23,
                "  Поддержание рациона  ",
                LocalDate.of(2026, 10, 23),
                NutritionPlan.Status.ACTIVE
        );

        assertEquals(
                "План #23: Поддержание рациона [ACTIVE], действует с 2026-10-23",
                NutritionPlanCaption.format(plan)
        );
        assertEquals("Поддержание рациона", plan.name());
        assertEquals(NutritionPlan.Status.ACTIVE, plan.status());
    }

    @Test
    void nullFromJavaFollowsExplicitKotlinContract() {
        var failure = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> NutritionPlanCaption.format(null)
        );

        assertEquals("План питания обязателен", failure.getMessage());
    }

    @Test
    void boundaryPlanKeepsZeroIdentifierAndReviewStatus() {
        var plan = new NutritionPlan(
                0,
                "Исторический план",
                LocalDate.of(2000, 1, 1),
                NutritionPlan.Status.NEEDS_REVIEW
        );

        var caption = NutritionPlanCaption.format(plan);

        assertEquals(
                "План #0: Исторический план [NEEDS_REVIEW], действует с 2000-01-01",
                caption
        );
        assertFalse(caption.isBlank());
    }

    @Test
    void javaSeesPredictablePublicStaticJvmSignature() throws Exception {
        var method = NutritionPlanCaption.class.getDeclaredMethod(
                "format", NutritionPlan.class);

        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertEquals(String.class, method.getReturnType());
    }
}
