package mx.udg.alumnos.tanken;

import java.util.*;

/** Checks every selected day before replacing a group, within one Room transaction. */
final class ScheduleEdits {
    private ScheduleEdits() {}

    static AcademicSchedule.Lesson replace(
            TankenDatabase db, String groupId, List<AcademicSchedule.Lesson> candidates) {
        return db.runInTransaction(
                () -> {
                    if (candidates.isEmpty())
                        throw new IllegalArgumentException("No days selected");
                    Set<Integer> days = new HashSet<>();
                    Set<String> ids = new HashSet<>();
                    List<AcademicSchedule.Lesson> outside = new ArrayList<>();
                    for (TankenDatabase.LessonRow row : db.records().lessons()) {
                        AcademicSchedule.Lesson existing = row.lesson();
                        if (!existing.groupId.equals(groupId)) outside.add(existing);
                    }
                    AcademicSchedule.Lesson first = candidates.get(0);
                    for (AcademicSchedule.Lesson lesson : candidates) {
                        if (!groupId.equals(lesson.groupId)
                                || !days.add(lesson.day.getValue())
                                || lesson.day.getValue() > 6
                                || !ids.add(lesson.id)
                                || !lesson.name.equals(first.name)
                                || !lesson.room.equals(first.room)
                                || !lesson.start.equals(first.start)
                                || !lesson.end.equals(first.end))
                            throw new IllegalArgumentException("Invalid lesson group");
                        for (AcademicSchedule.Lesson existing : outside)
                            if (existing.id.equals(lesson.id))
                                throw new IllegalArgumentException("Duplicate occurrence ID");
                        AcademicSchedule.Lesson conflict =
                                ScheduleValidator.conflict(outside, lesson);
                        if (conflict != null) return conflict;
                    }
                    db.records().deleteLessonGroup(groupId);
                    for (AcademicSchedule.Lesson lesson : candidates)
                        db.records().putLesson(new TankenDatabase.LessonRow(lesson));
                    return null;
                });
    }
}
