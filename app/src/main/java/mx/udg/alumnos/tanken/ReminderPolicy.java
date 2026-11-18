package mx.udg.alumnos.tanken;

import java.time.*;
import java.util.List;

/** Clock- and Android-independent scheduling decisions. Equal silence endpoints disable silence. */
public final class ReminderPolicy {
    private ReminderPolicy() {}

    public static ZonedDateTime afterClass(
            List<AcademicSchedule.Lesson> lessons, ZonedDateTime time) {
        AcademicSchedule.Occurrence current = AcademicSchedule.at(lessons, time).current;
        return current == null ? time : current.end;
    }

    public static ZonedDateTime nextAllowed(ZonedDateTime time, WellnessPreferences p) {
        for (int i = 0; i < 24; i++) {
            if ((p.dayMask & (1 << (time.getDayOfWeek().getValue() - 1))) == 0) {
                time = time.toLocalDate().plusDays(1).atStartOfDay(time.getZone());
                continue;
            }
            LocalTime hour = time.toLocalTime();
            if (p.silenceStart.equals(p.silenceEnd)) return time;
            boolean wraps = p.silenceStart.isAfter(p.silenceEnd);
            boolean silent =
                    wraps
                            ? !hour.isBefore(p.silenceStart) || hour.isBefore(p.silenceEnd)
                            : !hour.isBefore(p.silenceStart) && hour.isBefore(p.silenceEnd);
            if (!silent) return time;
            LocalDate day = time.toLocalDate();
            if (wraps && !hour.isBefore(p.silenceStart)) day = day.plusDays(1);
            time = day.atTime(p.silenceEnd).atZone(time.getZone());
        }
        throw new IllegalArgumentException("No allowed reminder time");
    }

    public static ZonedDateTime wellnessTime(
            List<AcademicSchedule.Lesson> lessons, ZonedDateTime time, WellnessPreferences p) {
        return wellnessTime(lessons, time, p, time.plusDays(8));
    }

    static ZonedDateTime wellnessTime(
            List<AcademicSchedule.Lesson> lessons,
            ZonedDateTime time,
            WellnessPreferences p,
            ZonedDateTime deadline) {
        while (time.isBefore(deadline)) {
            ZonedDateTime allowed = nextAllowed(time, p);
            if (!allowed.isBefore(deadline)) return deadline;
            ZonedDateTime free = afterClass(lessons, allowed);
            if (free.equals(allowed)) return free;
            time = free;
        }
        return deadline;
    }

    public static boolean enabled(String kind, WellnessPreferences p) {
        switch (kind) {
            case "HYDRATION":
                return p.hydrationEnabled;
            case "ACTIVE_BREAK":
                return p.breaksEnabled;
            case "CLASS":
                return p.classesEnabled;
            default:
                return false;
        }
    }
}
