package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import org.junit.Test;

public class MotionAccumulatorTest {
    @Test
    public void stationaryGravityIsNotMovement() {
        MotionAccumulator a = new MotionAccumulator();
        a.accept(0, 0, 9.81f, 1_000_000_000L);
        a.accept(0, 0, 9.81f, 2_000_000_000L);
        assertEquals(0, a.movement, 0.0001);
        assertEquals(1_000_000_000L, a.observedNanos);
    }

    @Test
    public void gapsDoNotCountAsObservedRest() {
        MotionAccumulator a = new MotionAccumulator();
        a.accept(0, 0, 9.81f, 1_000_000_000L);
        assertEquals(0, a.accept(10, 0, 0, 7_000_000_000L));
        assertEquals(0, a.observedNanos);
        assertEquals(0, a.movement, 0.0001);
    }

    @Test
    public void pausePreservesSessionButReinitializesGravity() {
        MotionAccumulator a = new MotionAccumulator();
        a.accept(0, 0, 9.81f, 1_000_000_000L);
        a.accept(3, 0, 9.81f, 1_100_000_000L);
        double peak = a.peak;
        assertTrue(peak > 0.8);
        a.disconnect();
        a.accept(0, 9.81f, 0, 10_000_000_000L);
        assertEquals(3, a.samples);
        assertEquals(100_000_000L, a.observedNanos);
        assertEquals(peak, a.peak, 0.0001);
    }

    @Test
    public void invalidSamplesCannotPolluteSession() {
        MotionAccumulator a = new MotionAccumulator();
        a.accept(0, 0, 9.81f, 10);
        assertEquals(-1, a.accept(Float.NaN, 0, 0, 20));
        assertEquals(-1, a.accept(0, 0, 0, 9));
        assertEquals(1, a.samples);
    }

    @Test
    public void resetClearsCountsAndPeak() {
        MotionAccumulator a = new MotionAccumulator();
        a.accept(1, 2, 3, 1);
        a.reset();
        assertEquals(0, a.samples);
        assertEquals(0, a.peak, 0);
        assertEquals(0, a.observedNanos);
    }
}
