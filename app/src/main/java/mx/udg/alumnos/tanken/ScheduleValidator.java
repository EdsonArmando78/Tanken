package mx.udg.alumnos.tanken;

import java.util.List;

public final class ScheduleValidator {
    private ScheduleValidator() {}

    public static AcademicSchedule.Lesson conflict(
            List<AcademicSchedule.Lesson> lessons, AcademicSchedule.Lesson candidate) {
        for (AcademicSchedule.Lesson other : lessons) {
            if (!other.id.equals(candidate.id)
                    && other.day == candidate.day
                    && candidate.start.isBefore(other.end)
                    && other.start.isBefore(candidate.end)) return other;
        }
        return null;
    }
}
