package mx.udg.alumnos.tanken;

import java.time.*;
import java.util.List;

/** Compact home content: today's next class and one relevant notice, never sample data. */
final class HomeSummary {
    private HomeSummary() {}

    static AcademicSchedule.Occurrence lesson(
            List<AcademicSchedule.Lesson> lessons, ZonedDateTime now) {
        AcademicSchedule.Status status = AcademicSchedule.at(lessons, now);
        if (status.next != null && status.next.start.toLocalDate().equals(now.toLocalDate()))
            return status.next;
        return status.current;
    }

    static long due(TankenDatabase.NoticeRow row) {
        return row.nextAttempt > 0 ? row.nextAttempt : row.originalDue;
    }

    static TankenDatabase.NoticeRow notice(
            List<TankenDatabase.NoticeRow> notices, ZonedDateTime now) {
        TankenDatabase.NoticeRow best = null;
        for (TankenDatabase.NoticeRow row : notices) {
            boolean issued = row.state.equals("ISSUED");
            if (!issued && !row.state.equals("SCHEDULED") && !row.state.equals("SNOOZED")) continue;
            if (row.expiresAt > 0 && row.expiresAt <= now.toInstant().toEpochMilli()) continue;
            if (due(row) <= 0) continue;
            if (!issued
                    && !Instant.ofEpochMilli(due(row))
                            .atZone(now.getZone())
                            .toLocalDate()
                            .equals(now.toLocalDate())) continue;
            if (best == null
                    || (issued && !best.state.equals("ISSUED"))
                    || (issued == best.state.equals("ISSUED") && due(row) < due(best))) best = row;
        }
        return best;
    }
}
