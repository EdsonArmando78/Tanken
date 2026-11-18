package mx.udg.alumnos.tanken;

import android.content.Context;

import java.time.format.DateTimeFormatter;
import java.util.*;

final class ComparisonReport {
    private ComparisonReport() {}

    static String format(Context c, WellnessSimulation.Comparison comparison) {
        DateTimeFormatter date =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z", Locale.getDefault());
        StringBuilder report =
                new StringBuilder(
                        c.getString(
                                R.string.wellness_report_header,
                                comparison.start.format(date),
                                comparison.end.format(date),
                                comparison.intervalMinutes,
                                comparison.lessonCount,
                                c.getString(
                                        comparison.simulatedInactivity
                                                ? R.string.wellness_inactive
                                                : R.string.wellness_active)));
        append(c, report, c.getString(R.string.wellness_fixed), comparison.fixed);
        append(c, report, c.getString(R.string.wellness_contextual), comparison.contextual);
        return report.toString();
    }

    private static void append(
            Context c, StringBuilder report, String title, List<WellnessSimulation.Event> events) {
        int emitted = 0, during = 0, rescheduled = 0;
        long delay = 0;
        for (WellnessSimulation.Event event : events) {
            if (!event.rescheduled.isEmpty()) rescheduled++;
            if (event.delivery != null) {
                emitted++;
                if (event.duringClass) during++;
                delay += event.delaySeconds();
            }
        }
        String proportion =
                emitted == 0
                        ? c.getString(R.string.wellness_not_calculable)
                        : String.format(Locale.getDefault(), "%.1f%%", 100.0 * during / emitted);
        String average =
                emitted == 0
                        ? c.getString(R.string.wellness_not_calculable)
                        : c.getString(R.string.wellness_seconds, (double) delay / emitted);
        report.append(
                c.getString(
                        R.string.wellness_mode_summary,
                        title,
                        events.size(),
                        emitted,
                        during,
                        proportion,
                        rescheduled,
                        events.size() - emitted,
                        average));
        DateTimeFormatter time = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss", Locale.getDefault());
        for (WellnessSimulation.Event event : events) {
            report.append(
                    c.getString(
                            R.string.wellness_event,
                            event.id,
                            RecordText.kind(c, event.kind.name()),
                            event.originalDue.format(time),
                            event.delivery == null
                                    ? c.getString(R.string.wellness_pending)
                                    : event.delivery.format(time)));
            for (java.time.ZonedDateTime change : event.rescheduled)
                report.append(c.getString(R.string.wellness_rescheduled, change.format(time)));
        }
    }
}
