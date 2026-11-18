package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.*;
import java.util.*;

public class ScheduleValidatorTest {
    private AcademicSchedule.Lesson lesson(String id, int start, int end) {
        return new AcademicSchedule.Lesson(
                id, "Clase", DayOfWeek.MONDAY, LocalTime.of(start, 0), LocalTime.of(end, 0));
    }

    @Test
    public void editingDoesNotConflictWithItself() {
        assertNull(
                ScheduleValidator.conflict(
                        Collections.singletonList(lesson("a", 8, 10)), lesson("a", 9, 11)));
    }

    @Test
    public void anotherClassConflictsAndOriginalIsUntouched() {
        AcademicSchedule.Lesson original = lesson("a", 8, 9), other = lesson("b", 10, 11);
        assertSame(
                other,
                ScheduleValidator.conflict(Arrays.asList(original, other), lesson("a", 9, 11)));
        assertEquals(LocalTime.of(8, 0), original.start);
    }

    @Test
    public void adjacentClassesAreAllowed() {
        assertNull(
                ScheduleValidator.conflict(
                        Collections.singletonList(lesson("a", 8, 9)), lesson("b", 9, 10)));
    }
}
