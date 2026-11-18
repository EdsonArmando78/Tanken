package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.*;
import java.util.*;

public class ReminderPolicyTest {
    private ZonedDateTime time(String value) {
        return ZonedDateTime.parse(value + "-06:00[America/Mexico_City]");
    }

    private WellnessPreferences prefs(int days, String start, String end) {
        return new WellnessPreferences(
                true, true, true, 60, 60, 10, days, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    public void overnightSilenceSkipsDisabledDays() {
        WellnessPreferences p = prefs(31, "22:00", "07:00");
        assertEquals(
                time("2026-11-23T07:00:00"),
                ReminderPolicy.nextAllowed(time("2026-11-20T22:30:00"), p));
        assertEquals(
                time("2026-11-16T07:00:00"),
                ReminderPolicy.nextAllowed(time("2026-11-16T06:00:00"), p));
    }

    @Test
    public void equalSilenceEndpointsMeanNoSilence() {
        ZonedDateTime now = time("2026-11-16T22:00:00");
        assertEquals(now, ReminderPolicy.nextAllowed(now, prefs(127, "22:00", "22:00")));
    }

    @Test
    public void adjacentClassesAndSilenceAreBothReevaluated() {
        List<AcademicSchedule.Lesson> lessons =
                Arrays.asList(
                        new AcademicSchedule.Lesson(
                                "Java", DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new AcademicSchedule.Lesson(
                                "Redes",
                                DayOfWeek.MONDAY,
                                LocalTime.of(10, 0),
                                LocalTime.of(11, 0)));
        assertEquals(
                time("2026-11-16T12:00:00"),
                ReminderPolicy.wellnessTime(
                        lessons, time("2026-11-16T09:30:00"), prefs(31, "10:30", "12:00")));
    }

    @Test
    public void noFreeWindowExpiresInsteadOfLoopingIntoAnotherWeek() {
        List<AcademicSchedule.Lesson> lessons =
                Collections.singletonList(
                        new AcademicSchedule.Lesson(
                                "Jornada",
                                DayOfWeek.MONDAY,
                                LocalTime.of(7, 0),
                                LocalTime.of(22, 0)));
        ZonedDateTime expiry = time("2026-11-17T00:00:00");
        assertEquals(
                expiry,
                ReminderPolicy.wellnessTime(
                        lessons, time("2026-11-16T08:00:00"), prefs(1, "22:00", "07:00"), expiry));
    }
}
