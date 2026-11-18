package mx.udg.alumnos.tanken;

import android.content.Context;

import org.json.*;

import java.time.*;
import java.util.*;

/** Transactional, retry-safe import; legacy preferences are preserved unchanged. */
final class LegacyScheduleImport {
    private LegacyScheduleImport() {}

    static String run(TankenDatabase db, Context context) {
        if (db.records().metadata("scheduleImported") != null) {
            String warning = db.records().metadata("importWarning");
            return warning;
        }
        String raw =
                context.getSharedPreferences("academic_schedule", Context.MODE_PRIVATE)
                        .getString("lessons", "[]");
        List<TankenDatabase.LessonRow> rows = new ArrayList<>();
        int invalid = 0;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject item = array.getJSONObject(i);
                    String id =
                            UUID.nameUUIDFromBytes(
                                            (i + ":" + item)
                                                    .getBytes(
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8))
                                    .toString();
                    rows.add(
                            new TankenDatabase.LessonRow(
                                    new AcademicSchedule.Lesson(
                                            id,
                                            item.getString("name"),
                                            DayOfWeek.of(item.getInt("day")),
                                            LocalTime.parse(item.getString("start")),
                                            LocalTime.parse(item.getString("end")))));
                } catch (JSONException | IllegalArgumentException | DateTimeException bad) {
                    invalid++;
                }
            }
        } catch (JSONException bad) {
            throw new IllegalStateException("Legacy schedule invalid; original preserved", bad);
        }
        final int rejected = invalid;
        db.runInTransaction(
                () -> {
                    for (TankenDatabase.LessonRow row : rows) db.records().putLesson(row);
                    db.records().putMetadata(new TankenDatabase.Metadata("scheduleImported", "1"));
                    if (rejected > 0)
                        db.records()
                                .putMetadata(
                                        new TankenDatabase.Metadata(
                                                "importWarning",
                                                context.getString(
                                                        R.string.import_warning, rejected)));
                });
        return invalid > 0 ? context.getString(R.string.import_warning, invalid) : null;
    }
}
