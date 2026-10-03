package ru.npi.kbju.lesson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import ru.npi.kbju.domain.DuplicatePlanException;
import ru.npi.kbju.domain.NutritionPlan;
import ru.npi.kbju.infrastructure.BackupException;
import ru.npi.kbju.infrastructure.SqliteBackupService;
import ru.npi.kbju.infrastructure.SqliteNutritionPlanRepository;

/** Выполняет воспроизводимый протокол снимка, изменения, восстановления и отказа. */
public final class BackupRecoveryDemo {
    private BackupRecoveryDemo() {
    }

    /** Создаёт только временные учебные данные и не открывает пользовательскую базу. */
    public static void main(String[] args) throws Exception {
        var root = Files.createTempDirectory("kbju-backup-recovery-");
        var workingDatabase = root.resolve("working/nutrition-plans.db");
        var backupDatabase = root.resolve("backup/nutrition-plans-v002.db");
        var restoredDatabase = root.resolve("restore/restored.db");
        var service = new SqliteBackupService();

        final List<NutritionPlan> stateAtSnapshot;
        final SqliteBackupService.BackupSnapshot snapshot;
        try (var repository = new SqliteNutritionPlanRepository(workingDatabase)) {
            repository.save(plan("Контроль массы", 1));
            repository.save(plan("Баланс КБЖУ", 2));
            stateAtSnapshot = repository.findAll();
            snapshot = service.create(workingDatabase, backupDatabase);
            repository.save(plan("Изменение после снимка", 3));
        }

        var sourceHashBeforeFailure = sha256(workingDatabase);
        var backupHashBeforeFailure = sha256(backupDatabase);
        var restore = service.restoreToNewDatabase(backupDatabase, restoredDatabase);

        final List<NutritionPlan> restoredState;
        try (var restored = new SqliteNutritionPlanRepository(restoredDatabase)) {
            restoredState = restored.findAll();
        }
        if (!stateAtSnapshot.equals(restoredState)) {
            throw new IllegalStateException("Восстановленные записи не совпадают со снимком");
        }
        if (restoredState.stream().anyMatch(
                plan -> plan.name().equals("Изменение после снимка"))) {
            throw new IllegalStateException("Старый снимок содержит более позднее изменение");
        }

        var damaged = root.resolve("damaged/not-a-database.db");
        Files.createDirectories(damaged.getParent());
        Files.writeString(damaged, "not sqlite", StandardCharsets.UTF_8);
        var rejectedTarget = root.resolve("restore/rejected.db");
        final BackupException damagedFailure;
        try {
            service.restoreToNewDatabase(damaged, rejectedTarget);
            throw new IllegalStateException("Повреждённый файл неожиданно восстановлен");
        } catch (BackupException exception) {
            damagedFailure = exception;
        }
        if (!sourceHashBeforeFailure.equals(sha256(workingDatabase))
                || !backupHashBeforeFailure.equals(sha256(backupDatabase))
                || Files.exists(rejectedTarget)) {
            throw new IllegalStateException("Отрицательный сценарий изменил защищённые данные");
        }

        System.out.println("Учебный каталог: " + root);
        System.out.println("Рабочая база: " + workingDatabase);
        System.out.println("Проверенный снимок: " + snapshot.path());
        System.out.println("Снимок: integrity=" + snapshot.inspection().integrityResult()
                + ", schema=" + snapshot.inspection().schemaVersion()
                + ", plans=" + snapshot.inspection().planCount());
        System.out.println("После снимка в рабочей базе: " + countPlans(workingDatabase)
                + "; в старой копии: " + snapshot.inspection().planCount());
        System.out.println("Восстановлено только в новый путь: " + restore.restoredPath());
        System.out.println("Сравнение записей: совпадают со состоянием снимка");
        System.out.println("Повреждённая копия: отказ " + damagedFailure.reason());
        System.out.println("Рабочая база и хороший снимок после отказа: не изменены");
        System.out.println("Учебный RPO: до 24 часов; изменения после последнего снимка могут быть потеряны");
    }

    private static NutritionPlan plan(String name, int day) {
        return new NutritionPlan(
                0,
                name,
                LocalDate.of(2026, 10, day),
                NutritionPlan.Status.DRAFT
        );
    }

    private static int countPlans(Path database) {
        try (var repository = new SqliteNutritionPlanRepository(database)) {
            return repository.findAll().size();
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен", exception);
        }
    }
}
