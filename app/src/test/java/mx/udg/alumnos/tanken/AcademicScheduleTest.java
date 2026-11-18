package mx.udg.alumnos.tanken;

import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class AcademicScheduleTest {
    private final AcademicSchedule.Lesson morning = new AcademicSchedule.Lesson("Matemáticas",
            DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0));
    private final AcademicSchedule.Lesson later = new AcademicSchedule.Lesson("Programación",
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
    private final List<AcademicSchedule.Lesson> lessons = Arrays.asList(later, morning);
    private ZonedDateTime monday(String time) {
        return ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.parse(time), ZoneId.of("America/Mexico_City"));
    }
    @Test public void emptyScheduleStillHasState() {
        AcademicSchedule.Status status = AcademicSchedule.at(Collections.emptyList(), monday("08:00"));
        assertEquals("Sin horarios registrados", status.dayState);
        assertNull(status.current);
        assertNull(status.next);
    }
    @Test public void beforeDayChoosesEarliestRegardlessOfOrder() {
        AcademicSchedule.Status status = AcademicSchedule.at(lessons, monday("07:59:59"));
        assertEquals("La jornada aún no comienza", status.dayState);
        assertSame(morning, status.next.lesson);
    }
    @Test public void startInclusiveAndEndExclusive() {
        AcademicSchedule.Status start = AcademicSchedule.at(lessons, monday("08:00"));
        assertSame(morning, start.current.lesson);
        assertSame(later, start.next.lesson);
        assertEquals("Clase en curso", start.dayState);
        AcademicSchedule.Status end = AcademicSchedule.at(lessons, monday("09:00"));
        assertNull(end.current);
        assertEquals("Descanso entre clases", end.dayState);
    }
    @Test public void endOfDayFindsNextWeek() {
        AcademicSchedule.Status status = AcademicSchedule.at(lessons, monday("11:00"));
        assertEquals("Jornada finalizada", status.dayState);
        assertSame(morning, status.next.lesson);
        assertEquals(LocalDate.of(2026, 10, 12), status.next.start.toLocalDate());
    }
    @Test public void freeDayAndSundayRollOver() {
        AcademicSchedule.Status status = AcademicSchedule.at(lessons, monday("12:00").plusDays(6));
        assertEquals("Hoy no hay clases", status.dayState);
        assertEquals(LocalDate.of(2026, 10, 12), status.next.start.toLocalDate());
    }
    @Test public void contiguousClassesSwitchAtBoundary() {
        AcademicSchedule.Lesson adjacent = new AcademicSchedule.Lesson("Historia", DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertSame(adjacent, AcademicSchedule.at(Arrays.asList(morning, adjacent), monday("09:00")).current.lesson);
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidInterval() {
        new AcademicSchedule.Lesson("Clase", DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON);
    }
}
