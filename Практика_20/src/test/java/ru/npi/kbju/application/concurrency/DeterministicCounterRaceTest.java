package ru.npi.kbju.application.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.RepeatedTest;

class DeterministicCounterRaceTest {
    @RepeatedTest(20)
    void barrierAlwaysExposesLostUpdateWhileAtomicCounterStaysExact() {
        var result = new DeterministicCounterRace(Duration.ofSeconds(2)).run();

        assertEquals(2, result.expectedCount());
        assertEquals(1, result.unsafeCount());
        assertEquals(2, result.atomicCount());
        assertTrue(result.executorTerminated());
    }
}
