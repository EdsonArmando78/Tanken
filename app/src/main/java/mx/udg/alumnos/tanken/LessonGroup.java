package mx.udg.alumnos.tanken;

import java.util.*;

/**
 * Presentation groups; the reminder/simulation engines still receive one weekly occurrence per day.
 */
final class LessonGroup {
    final AcademicSchedule.Lesson first;
    final int days;

    private LessonGroup(List<AcademicSchedule.Lesson> lessons) {
        first = lessons.get(0);
        int mask = 0;
        for (AcademicSchedule.Lesson lesson : lessons) mask |= 1 << (lesson.day.getValue() - 1);
        days = mask;
    }

    static List<LessonGroup> from(List<AcademicSchedule.Lesson> lessons) {
        Map<String, List<AcademicSchedule.Lesson>> groups = new LinkedHashMap<>();
        for (AcademicSchedule.Lesson lesson : lessons)
            groups.computeIfAbsent(lesson.groupId, id -> new ArrayList<>()).add(lesson);
        List<LessonGroup> result = new ArrayList<>();
        for (List<AcademicSchedule.Lesson> group : groups.values())
            result.add(new LessonGroup(group));
        return result;
    }
}
