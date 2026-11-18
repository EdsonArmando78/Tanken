package mx.udg.alumnos.tanken;

import org.json.*;

import java.util.List;

/** Versioned immutable payload inside the indexed Room comparison row. */
public final class ComparisonCodec {
    private ComparisonCodec() {}

    public static final class Input {
        public final List<AcademicSchedule.Lesson> lessons;
        public final java.time.ZonedDateTime start;
        public final int interval;
        public final boolean inactive;

        Input(
                List<AcademicSchedule.Lesson> lessons,
                java.time.ZonedDateTime start,
                int interval,
                boolean inactive) {
            this.lessons = java.util.Collections.unmodifiableList(lessons);
            this.start = start;
            this.interval = interval;
            this.inactive = inactive;
        }
    }

    public static Input input(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            if (json.getInt("formatVersion") != 1
                    || json.getInt("algorithmVersion") != 1
                    || !json.getString("origin").equals("SIMULATED"))
                throw new IllegalArgumentException("Unsupported comparison format");
            java.util.List<AcademicSchedule.Lesson> lessons = new java.util.ArrayList<>();
            JSONArray rows = json.getJSONArray("lessons");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                lessons.add(
                        new AcademicSchedule.Lesson(
                                row.getString("id"),
                                row.getString("name"),
                                java.time.DayOfWeek.of(row.getInt("day")),
                                java.time.LocalTime.parse(row.getString("start")),
                                java.time.LocalTime.parse(row.getString("end"))));
            }
            return new Input(
                    lessons,
                    java.time.ZonedDateTime.parse(json.getString("start")),
                    json.getInt("intervalMinutes"),
                    json.getBoolean("inactive"));
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Invalid comparison snapshot", invalid);
        }
    }

    public static WellnessSimulation.Comparison replay(String payload) {
        Input input = input(payload);
        return WellnessSimulation.compare(
                input.lessons, input.start, input.interval, input.inactive);
    }

    public static WellnessSimulation.Comparison read(String payload) {
        Input input = input(payload);
        try {
            JSONObject json = new JSONObject(payload);
            return new WellnessSimulation.Comparison(
                    input.start,
                    input.interval,
                    input.inactive,
                    input.lessons.size(),
                    readEvents(json.getJSONArray("fixed")),
                    readEvents(json.getJSONArray("contextual")));
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Invalid stored result", invalid);
        }
    }

    private static List<WellnessSimulation.Event> readEvents(JSONArray rows) throws JSONException {
        List<WellnessSimulation.Event> result = new java.util.ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            List<java.time.ZonedDateTime> changes = new java.util.ArrayList<>();
            JSONArray rescheduled = row.getJSONArray("rescheduled");
            for (int j = 0; j < rescheduled.length(); j++)
                changes.add(java.time.ZonedDateTime.parse(rescheduled.getString(j)));
            result.add(
                    new WellnessSimulation.Event(
                            row.getInt("id"),
                            WellnessSimulation.Kind.valueOf(row.getString("kind")),
                            java.time.ZonedDateTime.parse(row.getString("originalDue")),
                            row.isNull("delivery")
                                    ? null
                                    : java.time.ZonedDateTime.parse(row.getString("delivery")),
                            changes,
                            row.getBoolean("duringClass")));
        }
        return result;
    }

    public static String encode(
            List<AcademicSchedule.Lesson> lessons, WellnessSimulation.Comparison c) {
        try {
            JSONObject json = new JSONObject();
            json.put("formatVersion", 1)
                    .put("algorithmVersion", 1)
                    .put("origin", "SIMULATED")
                    .put("start", c.start.toString())
                    .put("end", c.end.toString())
                    .put("zone", c.start.getZone().getId())
                    .put("intervalMinutes", c.intervalMinutes)
                    .put("inactive", c.simulatedInactivity);
            JSONArray schedule = new JSONArray();
            for (AcademicSchedule.Lesson l : lessons)
                schedule.put(
                        new JSONObject()
                                .put("id", l.id)
                                .put("name", l.name)
                                .put("day", l.day.getValue())
                                .put("start", l.start.toString())
                                .put("end", l.end.toString()));
            return json.put("lessons", schedule)
                    .put("fixed", events(c.fixed))
                    .put("contextual", events(c.contextual))
                    .toString();
        } catch (JSONException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static JSONArray events(List<WellnessSimulation.Event> events) throws JSONException {
        JSONArray result = new JSONArray();
        for (WellnessSimulation.Event e : events) {
            JSONArray changes = new JSONArray();
            for (java.time.ZonedDateTime time : e.rescheduled) changes.put(time.toString());
            result.put(
                    new JSONObject()
                            .put("id", e.id)
                            .put("kind", e.kind.name())
                            .put("originalDue", e.originalDue.toString())
                            .put(
                                    "delivery",
                                    e.delivery == null ? JSONObject.NULL : e.delivery.toString())
                            .put("rescheduled", changes)
                            .put("duringClass", e.duringClass));
        }
        return result;
    }
}
