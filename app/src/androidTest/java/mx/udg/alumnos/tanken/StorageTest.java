package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.*;
import java.util.*;

/** Isolated database and preference names: never alters the student's saved schedule. */
@RunWith(AndroidJUnit4.class)
public class StorageTest {
    @Rule
    public androidx.room.testing.MigrationTestHelper migration =
            new androidx.room.testing.MigrationTestHelper(
                    InstrumentationRegistry.getInstrumentation(), TankenDatabase.class);

    private Context context;
    private TankenDatabase db;
    private String databaseName, preferenceName;

    @Before
    public void open() {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String suffix = UUID.randomUUID().toString();
        databaseName = "test-" + suffix + ".db";
        preferenceName = "test-" + suffix;
        context =
                new ContextWrapper(base) {
                    @Override
                    public SharedPreferences getSharedPreferences(String name, int mode) {
                        return super.getSharedPreferences(preferenceName, mode);
                    }
                };
        db = Room.databaseBuilder(base, TankenDatabase.class, databaseName).build();
    }

    @After
    public void close() {
        db.close();
        context.deleteDatabase(databaseName);
        context.deleteSharedPreferences(preferenceName);
    }

    @Test
    public void versionOneMigrationPreservesExistingSchedule() throws Exception {
        try (androidx.sqlite.db.SupportSQLiteDatabase old =
                migration.createDatabase(databaseName, 1)) {
            old.execSQL(
                    "INSERT INTO lessons (id,name,day,start,end,updatedAt) VALUES"
                            + " ('stable','Java',1,'08:00','09:00',100)");
            old.execSQL(
                    "INSERT INTO notices"
                        + " (id,kind,origin,state,reason,response,zone,createdAt,originalDue,nextAttempt,updatedAt)"
                        + " VALUES"
                        + " ('notice','HYDRATION','REAL','SCHEDULED','','','America/Mexico_City',100,200,200,100)");
        }
        try (androidx.sqlite.db.SupportSQLiteDatabase upgraded =
                        migration.runMigrationsAndValidate(
                                databaseName, 2, true, TankenDatabase.MIGRATION_1_2);
                android.database.Cursor cursor = upgraded.query("SELECT id FROM lessons")) {
            assertTrue(cursor.moveToFirst());
            assertEquals("stable", cursor.getString(0));
        }
    }

    @Test
    public void legacyImportSurvivesReopenAndNeverDuplicatesOrReplacesOriginal() {
        String raw =
                "[{\"name\":\"Java\",\"day\":1,\"start\":\"08:00\",\"end\":\"09:00\"},{\"name\":\"Broken\"}]";
        SharedPreferences old = context.getSharedPreferences("academic_schedule", 0);
        assertTrue(old.edit().putString("lessons", raw).commit());
        assertNotNull(LegacyScheduleImport.run(db, context));
        String id = db.records().lessons().get(0).id;
        assertEquals(raw, old.getString("lessons", ""));
        db.close();
        db = Room.databaseBuilder(context, TankenDatabase.class, databaseName).build();
        LegacyScheduleImport.run(db, context);
        assertEquals(1, db.records().lessons().size());
        assertEquals(id, db.records().lessons().get(0).id);
    }

    @Test
    public void malformedRootIsPreservedAndNotMarkedImported() {
        SharedPreferences old = context.getSharedPreferences("academic_schedule", 0);
        assertTrue(old.edit().putString("lessons", "not-json").commit());
        assertThrows(IllegalStateException.class, () -> LegacyScheduleImport.run(db, context));
        assertNull(db.records().metadata("scheduleImported"));
        assertEquals("not-json", old.getString("lessons", ""));
    }

    @Test
    public void transitionsAreIdempotentAndRetentionProtectsPendingNotices() {
        TankenDatabase.NoticeRow row = new TankenDatabase.NoticeRow();
        row.id = "notice";
        row.kind = "HYDRATION";
        row.originalDue = 100;
        row.nextAttempt = 100;
        db.records().insertNotice(row);
        assertTrue(
                NoticeTransitions.apply(
                        db,
                        row.id,
                        "class",
                        NoticeRules.State.SNOOZED,
                        NoticeRules.Reason.CLASS,
                        100,
                        200));
        assertFalse(
                NoticeTransitions.apply(
                        db,
                        row.id,
                        "class",
                        NoticeRules.State.SNOOZED,
                        NoticeRules.Reason.CLASS,
                        100,
                        300));
        assertEquals(100, db.records().notice(row.id).originalDue);
        assertEquals(200, db.records().notice(row.id).nextAttempt);
        db.records().pruneNotices(1000);
        assertNotNull(db.records().notice(row.id));
        assertTrue(
                NoticeTransitions.apply(
                        db,
                        row.id,
                        "issue",
                        NoticeRules.State.ISSUED,
                        NoticeRules.Reason.NONE,
                        200,
                        0));
        assertTrue(
                NoticeTransitions.apply(
                        db,
                        row.id,
                        "done",
                        NoticeRules.State.COMPLETED,
                        NoticeRules.Reason.USER,
                        201,
                        0));
        assertFalse(
                NoticeTransitions.apply(
                        db,
                        row.id,
                        "skip",
                        NoticeRules.State.SKIPPED,
                        NoticeRules.Reason.USER,
                        202,
                        0));
        db.records().pruneNotices(1000);
        assertNull(db.records().notice(row.id));
        assertEquals(0, db.records().actionCount(row.id, "done"));
    }

    @Test
    public void comparisonRoundTripIsReproducibleUniqueAndBounded() {
        List<AcademicSchedule.Lesson> lessons =
                Collections.singletonList(
                        new AcademicSchedule.Lesson(
                                "id",
                                "Java",
                                DayOfWeek.MONDAY,
                                LocalTime.of(8, 0),
                                LocalTime.of(10, 0)));
        WellnessSimulation.Comparison comparison =
                WellnessSimulation.compare(
                        lessons,
                        ZonedDateTime.parse("2026-11-16T07:00:00-06:00[America/Mexico_City]"),
                        60,
                        true);
        String payload = ComparisonCodec.encode(lessons, comparison);
        WellnessSimulation.Comparison replay = ComparisonCodec.replay(payload);
        assertEquals(payload, ComparisonCodec.encode(lessons, replay));
        db.runInTransaction(
                () -> {
                    for (int i = 0; i < 101; i++) {
                        TankenDatabase.ComparisonRow row = new TankenDatabase.ComparisonRow();
                        row.id = "comparison-" + i;
                        row.createdAt = 1000 + i;
                        row.inputAndResults = payload;
                        db.records().putComparison(row);
                        assertEquals(-1, db.records().putComparison(row));
                    }
                });
        db.records().pruneComparisons(0);
        assertEquals(100, db.records().comparisons().size());
        assertEquals(payload, db.records().comparisons().get(0).inputAndResults);
        db.records().pruneComparisons(2000);
        assertTrue(db.records().comparisons().isEmpty());
    }

    @Test
    public void versionTwoMigrationPreservesNoticesAndAddsCatalog() throws Exception {
        try (androidx.sqlite.db.SupportSQLiteDatabase old =
                migration.createDatabase(databaseName, 2)) {
            old.execSQL(
                    "INSERT INTO lessons (id,name,day,start,end,updatedAt) VALUES"
                        + " ('stable','Java',1,'08:00','09:00',100)");
        }
        try (androidx.sqlite.db.SupportSQLiteDatabase upgraded =
                        migration.runMigrationsAndValidate(
                                databaseName, 3, true, TankenDatabase.MIGRATION_2_3);
                android.database.Cursor cursor = upgraded.query("SELECT id FROM lessons")) {
            assertTrue(cursor.moveToFirst());
            assertEquals("stable", cursor.getString(0));
        }
    }

    private Catalog bundled() throws Exception {
        try (java.io.InputStream stream = context.getAssets().open("services.json")) {
            return CatalogCodec.read(stream);
        }
    }

    @Test
    public void catalogRejectsInvalidInputAndRollsBackFailedReplacement() throws Exception {
        Catalog seed = bundled();
        CatalogRepository.persist(db, seed, false, 0);
        assertThrows(
                java.io.IOException.class,
                () ->
                        CatalogCodec.read(
                                new java.io.ByteArrayInputStream(
                                        new byte[CatalogCodec.MAX_BYTES + 1])));
        String raw;
        try (java.io.InputStream stream = context.getAssets().open("services.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int n;
            while ((n = stream.read(bytes)) != -1) out.write(bytes, 0, n);
            raw = out.toString("UTF-8");
        }
        org.json.JSONObject json = new org.json.JSONObject(raw);
        org.json.JSONArray items = json.getJSONArray("services");
        items.put(items.getJSONObject(0));
        assertThrows(IllegalArgumentException.class, () -> CatalogCodec.parse(json.toString()));
        org.json.JSONObject unsafe = new org.json.JSONObject(raw);
        unsafe.getJSONArray("services").getJSONObject(0).put("source", "javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> CatalogCodec.parse(unsafe.toString()));
        Catalog duplicate =
                new Catalog(
                        "broken",
                        Arrays.asList(seed.services.get(0), seed.services.get(0)),
                        true,
                        1);
        assertThrows(
                RuntimeException.class, () -> CatalogRepository.persist(db, duplicate, true, 1));
        assertEquals(seed.version, db.records().catalogInfo().version);
        assertEquals(seed.services.size(), db.records().services().size());
        assertFalse(db.records().catalogInfo().imported);
    }

    @Test
    public void resetClearsRecordsAndRestoresBundledCatalogAtomically() throws Exception {
        Catalog seed = bundled();
        CatalogRepository.persist(db, new Catalog("custom", seed.services, true, 10), true, 10);
        TankenDatabase.LessonRow lesson = new TankenDatabase.LessonRow();
        lesson.id = "lesson";
        db.records().putLesson(lesson);
        TankenDatabase.NoticeRow notice = new TankenDatabase.NoticeRow();
        notice.id = "notice";
        db.records().insertNotice(notice);
        TankenDatabase.MotionRow motion = new TankenDatabase.MotionRow();
        motion.id = "motion";
        db.records().putMotion(motion);
        TankenDatabase.ComparisonRow comparison = new TankenDatabase.ComparisonRow();
        comparison.id = "comparison";
        db.records().putComparison(comparison);
        db.records().putMetadata(new TankenDatabase.Metadata("dataGeneration", "2"));
        db.records().putMetadata(new TankenDatabase.Metadata("resetPending", "1"));
        ResetData.clear(db, seed, 1000);
        assertTrue(db.records().lessons().isEmpty());
        assertTrue(db.records().notices().isEmpty());
        assertTrue(db.records().motion().isEmpty());
        assertTrue(db.records().comparisons().isEmpty());
        assertEquals(seed.version, db.records().catalogInfo().version);
        assertFalse(db.records().catalogInfo().imported);
        assertEquals("0", db.records().metadata("resetPending"));
        assertEquals("1", db.records().metadata("scheduleImported"));
        assertEquals("1", db.records().metadata("remindersDisabled"));
        assertEquals("2", db.records().metadata("dataGeneration"));
        assertEquals("1000", db.records().metadata("historyClearedAt"));
    }
}
