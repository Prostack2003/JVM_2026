package ru.npi.kbju.application.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.RepeatedTest;

class WaitingTaskCancellationTest {
    @RepeatedTest(10)
    void cancellationInterruptsWaitRestoresFlagAndLeavesNoTaskBehind() {
        var result = new WaitingTaskCancellation(Duration.ofSeconds(2)).run();

        assertTrue(result.cancellationRequested());
        assertTrue(result.futureCancelled());
        assertTrue(result.workerInterrupted());
        assertTrue(result.interruptFlagRestored());
        assertEquals(0, result.activeTasksAfterClose());
        assertTrue(result.executorTerminated());
    }
}
