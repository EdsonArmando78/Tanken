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

    @Test
    public void groupDisplaysTogetherButClockFindsBothDaysAndLegacyClassesStaySeparate() {
        AcademicSchedule.Lesson monday =
                new AcademicSchedule.Lesson(
                        "mon",
                        "Java",
                        DayOfWeek.MONDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        "group",
                        "D-101");
        AcademicSchedule.Lesson wednesday =
                new AcademicSchedule.Lesson(
                        "wed",
                        "Java",
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        "group",
                        "D-101");
        AcademicSchedule.Lesson separate =
                new AcademicSchedule.Lesson(
                        "old", "Java", DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 0));
        List<AcademicSchedule.Lesson> schedule = Arrays.asList(monday, wednesday, separate);
        List<LessonGroup> groups = LessonGroup.from(schedule);
        assertEquals(2, groups.size());
        assertEquals(5, groups.get(0).days);
        assertEquals("D-101", groups.get(0).first.room);
        ZonedDateTime start = ZonedDateTime.parse("2026-11-16T08:30:00-06:00[America/Mexico_City]");
        assertSame(monday, AcademicSchedule.at(schedule, start).current.lesson);
        assertSame(wednesday, AcademicSchedule.at(schedule, start.plusDays(2)).current.lesson);
    }
}
