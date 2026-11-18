package mx.udg.alumnos.tanken;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

/** Weekly schedule calculations, independent of Android and the clock UI. */
public final class AcademicSchedule {
    private AcademicSchedule() {}

    public static final class Lesson {
        public final String id;
        public final String name;
        public final DayOfWeek day;
        public final LocalTime start, end;

        public Lesson(String name, DayOfWeek day, LocalTime start, LocalTime end) {
            this(java.util.UUID.randomUUID().toString(), name, day, start, end);
        }

        public Lesson(String id, String name, DayOfWeek day, LocalTime start, LocalTime end) {
            if (name == null
                    || name.trim().isEmpty()
                    || day == null
                    || start == null
                    || end == null
                    || !end.isAfter(start)) {
                throw new IllegalArgumentException(
                        "La clase debe tener nombre y terminar después de iniciar.");
            }
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("Missing lesson ID");
            this.id = id;
            this.name = name.trim();
            this.day = day;
            this.start = start;
            this.end = end;
        }
    }

    public static final class Occurrence {
        public final Lesson lesson;
        public final ZonedDateTime start, end;

        Occurrence(Lesson lesson, ZonedDateTime start, ZonedDateTime end) {
            this.lesson = lesson;
            this.start = start;
            this.end = end;
        }
    }

    public static final class Status {
        public Occurrence current, next;
        public String dayState;
    }

    public static Status at(List<Lesson> lessons, ZonedDateTime now) {
        Status result = new Status();
        ZonedDateTime first = null, last = null;
        for (Lesson lesson : lessons) {
            int offset = Math.floorMod(lesson.day.getValue() - now.getDayOfWeek().getValue(), 7);
            ZonedDateTime start =
                    now.toLocalDate().plusDays(offset).atTime(lesson.start).atZone(now.getZone());
            ZonedDateTime end = start.toLocalDate().atTime(lesson.end).atZone(now.getZone());
            if (offset == 0) {
                if (first == null || start.isBefore(first)) first = start;
                if (last == null || end.isAfter(last)) last = end;
                if (!now.isBefore(start)
                        && now.isBefore(end)
                        && (result.current == null || end.isBefore(result.current.end))) {
                    result.current = new Occurrence(lesson, start, end);
                }
            }
            if (!start.isAfter(now)) {
                start = start.plusWeeks(1);
                end = end.plusWeeks(1);
            }
            if (result.next == null || start.isBefore(result.next.start)) {
                result.next = new Occurrence(lesson, start, end);
            }
        }
        result.dayState =
                lessons.isEmpty()
                        ? "Sin horarios registrados"
                        : first == null
                                ? "Hoy no hay clases"
                                : now.isBefore(first)
                                        ? "La jornada aún no comienza"
                                        : !now.isBefore(last)
                                                ? "Jornada finalizada"
                                                : result.current != null
                                                        ? "Clase en curso"
                                                        : "Descanso entre clases";
        return result;
    }
}
