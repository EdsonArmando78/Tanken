package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.*;
import java.util.*;

public class HomeSummaryTest {
    private final ZonedDateTime now =
            ZonedDateTime.parse("2026-11-18T14:45:00-06:00[America/Mexico_City]");

    private AcademicSchedule.Lesson lesson(String id, DayOfWeek day, int start, int end) {
        return new AcademicSchedule.Lesson(
                id, "Clase", day, LocalTime.of(start, 0), LocalTime.of(end, 0));
    }

    @Test
    public void homeShowsTodayAndKeepsLastClassUntilItEndsWithoutShowingNextWeek() {
        AcademicSchedule.Lesson current = lesson("current", DayOfWeek.WEDNESDAY, 14, 15);
        AcademicSchedule.Lesson next = lesson("next", DayOfWeek.WEDNESDAY, 15, 16);
        List<AcademicSchedule.Lesson> lessons = Arrays.asList(current, next);
        assertSame(next, HomeSummary.lesson(lessons, now).lesson);
        assertSame(current, HomeSummary.lesson(Collections.singletonList(current), now).lesson);
        assertNull(HomeSummary.lesson(lessons, now.withHour(16).withMinute(0)));
        assertNull(
                HomeSummary.lesson(
                        Collections.singletonList(lesson("tomorrow", DayOfWeek.THURSDAY, 8, 9)),
                        now));
        assertNull(HomeSummary.lesson(Collections.emptyList(), now));
    }

    private TankenDatabase.NoticeRow notice(String id, String state, ZonedDateTime due) {
        TankenDatabase.NoticeRow row = new TankenDatabase.NoticeRow();
        row.id = id;
        row.state = state;
        row.originalDue = due.toInstant().toEpochMilli();
        row.nextAttempt = row.originalDue;
        row.expiresAt = due.plusHours(2).toInstant().toEpochMilli();
        return row;
    }

    @Test
    public void noticePrefersUnansweredAndHidesEmptyExpiredClosedAndFutureDays() {
        TankenDatabase.NoticeRow future = notice("next", "SCHEDULED", now.plusMinutes(65));
        TankenDatabase.NoticeRow issued = notice("issued", "ISSUED", now.minusMinutes(5));
        assertSame(issued, HomeSummary.notice(Arrays.asList(future, issued), now));
        assertSame(future, HomeSummary.notice(Collections.singletonList(future), now));
        assertNull(
                HomeSummary.notice(
                        Arrays.asList(
                                notice("expired", "ISSUED", now.minusHours(3)),
                                notice("done", "COMPLETED", now.minusMinutes(1)),
                                notice("tomorrow", "SCHEDULED", now.plusDays(1))),
                        now));
        assertNull(HomeSummary.notice(Collections.emptyList(), now));
    }
}
