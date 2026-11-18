package mx.udg.alumnos.tanken;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reproducible local experiment; these events are not measurements or scheduled alarms. */
public final class WellnessSimulation {
    private WellnessSimulation() {}

    public enum Kind {
        HYDRATION,
        ACTIVE_BREAK
    }

    public static final class Event {
        public final int id;
        public final Kind kind;
        public final ZonedDateTime originalDue, delivery;
        public final List<ZonedDateTime> rescheduled;
        public final boolean duringClass;

        Event(
                int id,
                Kind kind,
                ZonedDateTime due,
                ZonedDateTime delivery,
                List<ZonedDateTime> rescheduled,
                boolean duringClass) {
            this.id = id;
            this.kind = kind;
            originalDue = due;
            this.delivery = delivery;
            this.rescheduled = Collections.unmodifiableList(new ArrayList<>(rescheduled));
            this.duringClass = duringClass;
        }

        public long delaySeconds() {
            return delivery == null ? 0 : Duration.between(originalDue, delivery).getSeconds();
        }
    }

    public static final class Comparison {
        public final ZonedDateTime start, end;
        public final List<Event> fixed, contextual;
        public final boolean simulatedInactivity;
        public final int intervalMinutes, lessonCount;

        Comparison(
                ZonedDateTime start,
                int interval,
                boolean inactive,
                int lessonCount,
                List<Event> fixed,
                List<Event> contextual) {
            this.start = start;
            end = start.plusHours(8);
            intervalMinutes = interval;
            simulatedInactivity = inactive;
            this.lessonCount = lessonCount;
            this.fixed = Collections.unmodifiableList(fixed);
            this.contextual = Collections.unmodifiableList(contextual);
        }
    }

    public static Comparison compare(
            List<AcademicSchedule.Lesson> lessons,
            ZonedDateTime start,
            int intervalMinutes,
            boolean inactive) {
        if (intervalMinutes < 5 || intervalMinutes > 240) {
            throw new IllegalArgumentException("El intervalo debe estar entre 5 y 240 minutos.");
        }
        List<AcademicSchedule.Lesson> snapshot = new ArrayList<>(lessons);
        List<Event> fixed = new ArrayList<>(), contextual = new ArrayList<>();
        ZonedDateTime end = start.plusHours(8);
        int id = 1;
        for (ZonedDateTime due = start.plusMinutes(intervalMinutes);
                !due.isAfter(end);
                due = due.plusMinutes(intervalMinutes)) {
            for (Kind kind : Kind.values()) {
                // Activity is an explicit synthetic scenario, never inferred from missing sensor
                // data.
                if (kind == Kind.ACTIVE_BREAK && !inactive) continue;
                boolean inClass = AcademicSchedule.at(snapshot, due).current != null;
                fixed.add(new Event(id, kind, due, due, Collections.emptyList(), inClass));
                ZonedDateTime candidate = due;
                List<ZonedDateTime> changes = new ArrayList<>();
                // Reevaluate at every class end, including adjacent classes. Preserve original due.
                while (!candidate.isAfter(end)) {
                    AcademicSchedule.Occurrence current =
                            AcademicSchedule.at(snapshot, candidate).current;
                    if (current == null) break;
                    candidate = ReminderPolicy.afterClass(snapshot, candidate);
                    changes.add(candidate);
                }
                contextual.add(
                        new Event(
                                id++,
                                kind,
                                due,
                                candidate.isAfter(end) ? null : candidate,
                                changes,
                                false));
            }
        }
        return new Comparison(start, intervalMinutes, inactive, snapshot.size(), fixed, contextual);
    }
}
