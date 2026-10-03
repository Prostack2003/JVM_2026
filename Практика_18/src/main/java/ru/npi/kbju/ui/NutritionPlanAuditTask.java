package ru.npi.kbju.ui;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.concurrent.Task;
import ru.npi.kbju.application.audit.AuditExecutionException;
import ru.npi.kbju.application.audit.AuditResult;
import ru.npi.kbju.application.audit.NutritionPlanAuditor;
import ru.npi.kbju.domain.NutritionPlan;

/** Адаптирует независимый аудитор к наблюдаемому жизненному циклу JavaFX Task. */
public final class NutritionPlanAuditTask extends Task<AuditResult> {
    private static final Duration DEFAULT_STEP_DELAY = Duration.ofMillis(140);

    private final List<NutritionPlan> snapshot;
    private final NutritionPlanAuditor auditor;

    /** Создаёт одноразовую задачу для неизменяемого снимка. */
    public NutritionPlanAuditTask(List<NutritionPlan> plans, boolean controlledFailure) {
        this(plans, createAuditor(controlledFailure));
    }

    /** Позволяет проверкам подменить длительную операцию без запуска окна. */
    NutritionPlanAuditTask(List<NutritionPlan> plans, NutritionPlanAuditor auditor) {
        this.snapshot = List.copyOf(plans);
        this.auditor = auditor;
    }

    /** Выполняется в рабочем потоке и не обращается к визуальным узлам. */
    @Override
    protected AuditResult call() throws Exception {
        int total = NutritionPlanAuditor.totalChecks(snapshot.size());
        updateProgress(0, Math.max(1, total));
        updateMessage("Подготовлен снимок: " + snapshot.size() + " план(а)");
        return auditor.audit(snapshot, this::isCancelled, progress -> {
            updateProgress(progress.completed(), progress.total());
            updateMessage("Шаг " + progress.completed() + " из " + progress.total()
                    + ": " + progress.checkName());
        });
    }

    private static NutritionPlanAuditor createAuditor(boolean controlledFailure) {
        var pauseNumber = new AtomicInteger();
        return new NutritionPlanAuditor(DEFAULT_STEP_DELAY, duration -> {
            Thread.sleep(duration);
            if (controlledFailure && pauseNumber.incrementAndGet() == 4) {
                throw new AuditExecutionException(
                        "Учебный отказ на четвёртом шаге; данные не изменены");
            }
        });
    }
}
