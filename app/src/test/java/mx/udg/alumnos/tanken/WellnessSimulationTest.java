package mx.udg.alumnos.tanken;

import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class WellnessSimulationTest {
    private final ZonedDateTime start = ZonedDateTime.parse("2026-11-09T08:00:00-06:00[America/Mexico_City]");

    private AcademicSchedule.Lesson lesson(String from, String to) {
        return new AcademicSchedule.Lesson("Clase", DayOfWeek.MONDAY,
                LocalTime.parse(from), LocalTime.parse(to));
    }

    @Test public void noScheduleProducesIdenticalEventsAndBothTypesForInactivity() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(Collections.emptyList(), start, 60, true);
        assertEquals(16, result.fixed.size());
        for (int i = 0; i < result.fixed.size(); i++) {
            WellnessSimulation.Event fixed = result.fixed.get(i), context = result.contextual.get(i);
            assertEquals(fixed.id, context.id);
            assertEquals(fixed.originalDue, context.originalDue);
            assertEquals(fixed.delivery, context.delivery);
            assertFalse(context.duringClass);
            assertEquals(0, context.delaySeconds());
        }
        assertEquals(WellnessSimulation.Kind.ACTIVE_BREAK, result.fixed.get(1).kind);
    }

    @Test public void adjacentClassesAreReevaluatedWithoutChangingOriginalDueOrId() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(
                Arrays.asList(lesson("09:00", "10:00"), lesson("10:00", "11:00")), start, 60, false);
        WellnessSimulation.Event fixed = result.fixed.get(0), context = result.contextual.get(0);
        assertTrue(fixed.duringClass); // Inclusive class start at 09:00.
        assertEquals(start.plusHours(1), context.originalDue);
        assertEquals(start.plusHours(3), context.delivery);
        assertEquals(Arrays.asList(start.plusHours(2), start.plusHours(3)), context.rescheduled);
        assertEquals(7200, context.delaySeconds());
        assertEquals(fixed.id, context.id);
        assertFalse(context.duringClass);
        assertFalse(result.fixed.get(2).duringClass); // Exclusive end at 11:00.
        assertEquals(result.fixed.get(2).delivery, result.contextual.get(2).delivery);
    }

    @Test public void eventsBeyondObservationRemainPendingAndStillHaveTheirDeadline() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(
                Collections.singletonList(lesson("08:00", "18:00")), start, 60, false);
        assertEquals(8, result.fixed.size());
        for (WellnessSimulation.Event event : result.contextual) {
            assertNull(event.delivery);
            assertNotNull(event.originalDue);
            assertEquals(start.plusHours(10), event.rescheduled.get(0));
        }
    }

    @Test public void movementScenarioKeepsHydrationAndDoesNotInventInactivity() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(Collections.emptyList(), start, 30, false);
        assertEquals(16, result.fixed.size());
        for (WellnessSimulation.Event event : result.fixed) {
            assertEquals(WellnessSimulation.Kind.HYDRATION, event.kind);
        }
    }

    @Test public void simulationCrossesMidnightAndUsesNextDaysSchedule() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(
                Collections.singletonList(lesson("00:00", "02:00")), start.minusHours(9), 60, false);
        assertTrue(result.fixed.get(0).duringClass);
        assertEquals(start.minusHours(6), result.contextual.get(0).delivery);
    }

    @Test public void snapshotsCannotBeMutatedByObservers() {
        WellnessSimulation.Comparison result = WellnessSimulation.compare(
                Collections.singletonList(lesson("09:00", "10:00")), start, 60, false);
        assertThrows(UnsupportedOperationException.class, () -> result.fixed.clear());
        assertThrows(UnsupportedOperationException.class, () -> result.contextual.get(0).rescheduled.clear());
    }

    @Test public void rejectsIntervalsOutsideBounds() {
        for (int interval : new int[] {-1, 0, 4, 241, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> WellnessSimulation.compare(
                    Collections.emptyList(), start, interval, false));
        }
    }
}
