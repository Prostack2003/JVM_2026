package ru.npi.kbju.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.npi.kbju.ApplicationContext;
import ru.npi.kbju.domain.NutritionPlan;

/** Воспроизводит четыре асинхронных UI-сценария и сохраняет снимки. */
public final class AuditEvidenceCapture extends Application {
    private static final long TIMEOUT_NANOS = java.time.Duration.ofSeconds(12).toNanos();
    private static volatile Throwable failure;

    private ApplicationContext context;
    private Path outputDirectory;
    private Stage stage;
    private MainController controller;

    /** Загружает тот же FXML, что и штатное приложение. */
    @Override
    public void start(Stage primaryStage) {
        try {
            outputDirectory = Path.of(getParameters().getRaw().getFirst());
            Files.createDirectories(outputDirectory);
            var database = Files.createTempDirectory("kbju-audit-evidence-")
                    .resolve("nutrition-plans.db");
            context = ApplicationContext.create(database);
            context.service().addPlan(
                    "План для аудита",
                    LocalDate.of(2026, 11, 1),
                    NutritionPlan.Status.NEEDS_REVIEW);

            var loaded = DesktopApp.loadMainView(context.service());
            controller = loaded.controller();
            stage = primaryStage;
            stage.setTitle("Практика 13 — проверка фонового аудита");
            stage.setScene(new Scene(loaded.root(), 1180, 820));
            stage.getScene().getStylesheets().add(loaded.stylesheet());
            stage.show();
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();

            capture("audit-initial.png");
            runSuccessScenario();
        } catch (Throwable throwable) {
            failAndExit(throwable);
        }
    }

    private void runSuccessScenario() {
        var fxPulses = new AtomicInteger();
        var pulseTimeline = new Timeline(new KeyFrame(
                Duration.millis(25), ignored -> fxPulses.incrementAndGet()));
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);

        controller.useControlledAuditFailure(false);
        controller.startAuditForEvidence();
        pulseTimeline.play();
        waitUntil("прогресс успешного запуска",
                () -> controller.auditRunning() && controller.auditProgress() > 0.05,
                () -> checked(() -> {
                    require(!controller.auditStartEnabled(),
                            "Повторный запуск должен быть заблокирован");
                    require(controller.auditCancelEnabled(),
                            "Отмена должна быть доступна во время работы");
                    capture("audit-running.png");
                    waitUntil("успешное завершение",
                            () -> !controller.auditRunning() && controller.auditRunCount() == 1
                                    && controller.auditStartEnabled(),
                            () -> checked(() -> {
                                pulseTimeline.stop();
                                require(fxPulses.get() >= 5,
                                        "JavaFX-цикл должен обрабатывать события во время аудита");
                                require(controller.auditProgress() == 1.0,
                                        "Успешный аудит должен завершить прогресс");
                                require(controller.auditResultText().startsWith("Подтверждено:"),
                                        "Только успех должен опубликовать итог");
                                capture("audit-succeeded.png");
                                System.out.println("Успех: прогресс=100%; FX-событий=" + fxPulses.get());
                                runCancellationScenario(controller.auditResultText());
                            }));
                }));
    }

    private void runCancellationScenario(String confirmedResult) {
        controller.useControlledAuditFailure(false);
        controller.startAuditForEvidence();
        waitUntil("точка отмены",
                () -> controller.auditRunning() && controller.auditProgress() > 0.05,
                () -> {
                    controller.cancelAuditForEvidence();
                    waitUntil("завершение отмены",
                            () -> !controller.auditRunning() && controller.auditRunCount() == 2
                                    && controller.auditStartEnabled(),
                            () -> checked(() -> {
                                require(controller.auditStatusText().contains("отменён"),
                                        "Отмена должна быть отдельным исходом");
                                require(confirmedResult.equals(controller.auditResultText()),
                                        "Отмена не должна заменять подтверждённый итог частичным");
                                require(controller.auditStartEnabled(),
                                        "После отмены нужен повторный запуск");
                                capture("audit-cancelled.png");
                                System.out.println("Отмена: частичный итог не опубликован; повтор доступен");
                                runFailureScenario(confirmedResult);
                            }));
                });
    }

    private void runFailureScenario(String confirmedResult) {
        controller.useControlledAuditFailure(true);
        controller.startAuditForEvidence();
        waitUntil("контролируемый отказ",
                () -> !controller.auditRunning() && controller.auditRunCount() == 3
                        && controller.auditStartEnabled(),
                () -> checked(() -> {
                    require(controller.auditStatusText().startsWith("Ошибка аудита:"),
                            "Отказ должен сохранить исходную причину");
                    require(controller.auditStatusText().contains("четвёртом шаге"),
                            "Пользователю нужно понятное сообщение отказа");
                    require(confirmedResult.equals(controller.auditResultText()),
                            "Отказ не должен публиковать промежуточный итог");
                    require(controller.auditStartEnabled(),
                            "После отказа нужен повторный запуск");
                    capture("audit-failed.png");
                    System.out.println("Отказ: причина показана; подтверждённый итог сохранён");
                    runRepeatedScenario();
                }));
    }

    private void runRepeatedScenario() {
        controller.useControlledAuditFailure(false);
        controller.startAuditForEvidence();
        waitUntil("повторный запуск",
                () -> !controller.auditRunning() && controller.auditRunCount() == 4
                        && controller.auditStartEnabled(),
                () -> checked(() -> {
                    require(controller.auditStatusText().contains("успешно"),
                            "Новая Task должна завершиться после отмены и отказа");
                    require(controller.auditRunCount() == 4,
                            "Каждый запуск должен создавать новую Task");
                    capture("audit-repeated.png");
                    System.out.println("Повтор: новая Task завершилась успешно; запусков=4");
                    Platform.exit();
                }));
    }

    private void waitUntil(String description, BooleanSupplier condition, Runnable next) {
        waitUntil(description, condition, next, System.nanoTime() + TIMEOUT_NANOS);
    }

    private void waitUntil(
            String description,
            BooleanSupplier condition,
            Runnable next,
            long deadline
    ) {
        try {
            if (condition.getAsBoolean()) {
                Platform.runLater(next);
                return;
            }
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Не достигнуто состояние: " + description);
            }
            var pause = new PauseTransition(Duration.millis(25));
            pause.setOnFinished(ignored -> waitUntil(description, condition, next, deadline));
            pause.play();
        } catch (Throwable throwable) {
            failAndExit(throwable);
        }
    }

    private void capture(String fileName) throws IOException {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        var root = stage.getScene().getRoot();
        var image = new WritableImage(
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getWidth())),
                Math.max(1, (int) Math.ceil(root.getBoundsInLocal().getHeight())));
        root.snapshot(null, image);
        var buffered = new BufferedImage(
                (int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (var y = 0; y < buffered.getHeight(); y++) {
            for (var x = 0; x < buffered.getWidth(); x++) {
                buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(buffered, "png", outputDirectory.resolve(fileName).toFile());
    }

    private static void checked(CheckedAction action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            failAndExit(throwable);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void failAndExit(Throwable throwable) {
        failure = throwable;
        Platform.exit();
    }

    /** Закрывает отдельную временную базу сценария. */
    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    static Throwable failure() {
        return failure;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
