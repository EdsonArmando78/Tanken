package mx.udg.alumnos.tanken;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.*;

import java.util.List;

/** All access is mediated by AppStore, never by a screen. */
@Database(
        entities = {
            TankenDatabase.LessonRow.class,
            TankenDatabase.NoticeRow.class,
            TankenDatabase.TransitionRow.class,
            TankenDatabase.MotionRow.class,
            TankenDatabase.ComparisonRow.class,
            TankenDatabase.Metadata.class,
            TankenDatabase.ServiceRow.class,
            TankenDatabase.CatalogInfo.class
        },
        version = 4,
        exportSchema = true)
public abstract class TankenDatabase extends RoomDatabase {
    public static final androidx.room.migration.Migration MIGRATION_1_2 =
            new androidx.room.migration.Migration(1, 2) {
                @Override
                public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                    for (String field : new String[] {"lessonId", "lessonName", "publication"})
                        db.execSQL(
                                "ALTER TABLE notices ADD COLUMN "
                                        + field
                                        + " TEXT NOT NULL DEFAULT ''");
                    for (String field :
                            new String[] {
                                "targetStart",
                                "expiresAt",
                                "receivedAt",
                                "emittedAt",
                                "answeredAt",
                                "revision",
                                "userNotBefore"
                            })
                        db.execSQL(
                                "ALTER TABLE notices ADD COLUMN "
                                        + field
                                        + " INTEGER NOT NULL DEFAULT 0");
                }
            };

    public static final androidx.room.migration.Migration MIGRATION_2_3 =
            new androidx.room.migration.Migration(2, 3) {
                @Override
                public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS services (id TEXT NOT NULL PRIMARY KEY,"
                                + " name TEXT NOT NULL, location TEXT NOT NULL, phone TEXT NOT"
                                + " NULL, extension TEXT NOT NULL, email TEXT NOT NULL, hours TEXT"
                                + " NOT NULL, source TEXT NOT NULL, reviewedAt TEXT NOT NULL)");
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS catalog_info (id INTEGER NOT NULL PRIMARY"
                                    + " KEY, version TEXT NOT NULL, imported INTEGER NOT NULL,"
                                    + " importedAt INTEGER NOT NULL)");
                }
            };

    public static final androidx.room.migration.Migration MIGRATION_3_4 =
            new androidx.room.migration.Migration(3, 4) {
                @Override
                public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE lessons ADD COLUMN groupId TEXT NOT NULL DEFAULT ''");
                    db.execSQL("ALTER TABLE lessons ADD COLUMN room TEXT NOT NULL DEFAULT ''");
                    db.execSQL("UPDATE lessons SET groupId=id");
                }
            };

    @Entity(tableName = "services")
    public static class ServiceRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull
        public String name = "",
                location = "",
                phone = "",
                extension = "",
                email = "",
                hours = "",
                source = "",
                reviewedAt = "";

        public ServiceRow() {}

        @Ignore
        public ServiceRow(Catalog.Service s) {
            id = s.id;
            name = s.name;
            location = s.location;
            phone = s.phone;
            extension = s.extension;
            email = s.email;
            hours = s.hours;
            source = s.source;
            reviewedAt = s.reviewedAt;
        }

        public Catalog.Service service() {
            return new Catalog.Service(
                    id, name, location, phone, extension, email, hours, source, reviewedAt);
        }
    }

    @Entity(tableName = "catalog_info")
    public static class CatalogInfo {
        @PrimaryKey public int id = 1;
        @NonNull public String version = "";
        public boolean imported;
        public long importedAt;
    }

    public abstract Records records();

    @Entity(tableName = "lessons", indices = @Index(value = {"day", "start"}))
    public static class LessonRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull public String name = "";
        @NonNull
        @ColumnInfo(defaultValue = "''")
        public String groupId = "", room = "";
        public int day;
        @NonNull public String start = "", end = "";
        public long updatedAt;

        public LessonRow() {}

        @Ignore
        public LessonRow(AcademicSchedule.Lesson l) {
            id = l.id;
            name = l.name;
            groupId = l.groupId;
            room = l.room;
            day = l.day.getValue();
            start = l.start.toString();
            end = l.end.toString();
            updatedAt = System.currentTimeMillis();
        }

        public AcademicSchedule.Lesson lesson() {
            return new AcademicSchedule.Lesson(
                    id,
                    name,
                    java.time.DayOfWeek.of(day),
                    java.time.LocalTime.parse(start),
                    java.time.LocalTime.parse(end),
                    groupId.isEmpty() ? id : groupId,
                    room);
        }
    }

    @Entity(
            tableName = "notices",
            indices = {@Index("state"), @Index("originalDue")})
    public static class NoticeRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull public String kind = "", origin = "REAL", state = "SCHEDULED";
        @NonNull public String reason = "", response = "", zone = "";
        public long createdAt, originalDue, nextAttempt, updatedAt;
        @ColumnInfo(defaultValue = "''")
        @NonNull
        public String lessonId = "", lessonName = "", publication = "";
        @ColumnInfo(defaultValue = "0")
        public long targetStart,
                expiresAt,
                receivedAt,
                emittedAt,
                answeredAt,
                revision,
                userNotBefore;
    }

    @Entity(
            tableName = "notice_transitions",
            foreignKeys =
                    @ForeignKey(
                            entity = NoticeRow.class,
                            parentColumns = "id",
                            childColumns = "noticeId",
                            onDelete = ForeignKey.CASCADE),
            indices = {
                @Index("noticeId"),
                @Index(
                        value = {"noticeId", "actionId"},
                        unique = true)
            })
    public static class TransitionRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull public String noticeId = "", actionId = "", state = "", reason = "";
        public long at, nextAttempt;
    }

    @Entity(tableName = "motion_summaries", indices = @Index("endedAt"))
    public static class MotionRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull public String sessionId = "", origin = "PHONE_ACCELEROMETER", unit = "m/s²";
        public long startedAt, endedAt, observedMillis, samples;
        public double peak;
    }

    @Entity(tableName = "comparisons", indices = @Index("createdAt"))
    public static class ComparisonRow {
        @PrimaryKey @NonNull public String id = "";
        public long createdAt;
        public int formatVersion = 1, algorithmVersion = 1;
        @NonNull public String origin = "SIMULATED", zone = "", inputAndResults = "";
    }

    @Entity(tableName = "metadata")
    public static class Metadata {
        @PrimaryKey @NonNull public String key = "";
        @NonNull public String value = "";

        public Metadata() {}

        @Ignore
        public Metadata(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @Dao
    public interface Records {
        @Query("SELECT * FROM services ORDER BY name")
        List<ServiceRow> services();

        @Query("SELECT * FROM catalog_info WHERE id=1")
        CatalogInfo catalogInfo();

        @Insert
        void putService(ServiceRow row);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void putCatalogInfo(CatalogInfo info);

        @Query("DELETE FROM services")
        void deleteServices();

        @Query("DELETE FROM lessons")
        void deleteAllLessons();

        @Query(
                "SELECT * FROM notices WHERE state NOT IN ('COMPLETED','SKIPPED','CANCELLED') ORDER"
                        + " BY nextAttempt, originalDue")
        List<NoticeRow> openNotices();

        @Query(
                "SELECT * FROM notices WHERE state NOT IN ('COMPLETED','SKIPPED','CANCELLED') ORDER"
                        + " BY originalDue")
        LiveData<List<NoticeRow>> observeOpenNotices();

        @Query("SELECT * FROM notice_transitions WHERE noticeId = :id ORDER BY at")
        List<TransitionRow> transitions(String id);

        @Query("SELECT * FROM comparisons WHERE id = :id")
        ComparisonRow comparison(String id);

        @Query("SELECT * FROM motion_summaries WHERE id = :id")
        MotionRow motion(String id);

        @Query(
                "SELECT * FROM (SELECT id, 'NOTICE' AS category, kind, state, origin, originalDue"
                    + " AS at, updatedAt AS revision FROM notices UNION ALL SELECT id, 'MOTION',"
                    + " 'MOTION', '', origin, endedAt, endedAt FROM motion_summaries UNION ALL"
                    + " SELECT id, 'COMPARISON', 'COMPARISON', '', origin, createdAt, createdAt"
                    + " FROM comparisons) WHERE at >= :from AND at < :until AND (:category = 'ALL'"
                    + " OR category = :category) ORDER BY at DESC, id")
        LiveData<List<HistoryRow>> history(long from, long until, String category);

        @Query(
                "DELETE FROM notices WHERE originalDue >= :from AND originalDue < :until AND state"
                        + " IN ('COMPLETED','SKIPPED','CANCELLED')")
        void deleteClosedNotices(long from, long until);

        @Query("DELETE FROM motion_summaries WHERE endedAt >= :from AND endedAt < :until")
        void deleteMotion(long from, long until);

        @Query("DELETE FROM comparisons WHERE createdAt >= :from AND createdAt < :until")
        void deleteComparisons(long from, long until);

        @Query("DELETE FROM notices")
        void deleteAllNotices();

        @Query("SELECT * FROM lessons ORDER BY day, start")
        LiveData<List<LessonRow>> observeLessons();

        @Query("SELECT * FROM lessons ORDER BY day, start")
        List<LessonRow> lessons();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void putLesson(LessonRow row);

        @Query("DELETE FROM lessons WHERE groupId = :id OR (groupId = '' AND id = :id)")
        void deleteLessonGroup(String id);

        @Query("SELECT value FROM metadata WHERE `key` = :key")
        String metadata(String key);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void putMetadata(Metadata row);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insertNotice(NoticeRow row);

        @Update
        void updateNotice(NoticeRow row);

        @Query("SELECT * FROM notices WHERE id = :id")
        NoticeRow notice(String id);

        @Query("SELECT * FROM notices ORDER BY originalDue DESC")
        List<NoticeRow> notices();

        @Query(
                "SELECT COUNT(*) FROM notice_transitions WHERE noticeId = :id AND actionId ="
                        + " :action")
        int actionCount(String id, String action);

        @Insert
        void transition(TransitionRow row);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void putMotion(MotionRow row);

        @Query("SELECT * FROM motion_summaries ORDER BY endedAt DESC")
        List<MotionRow> motion();

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long putComparison(ComparisonRow row);

        @Query("SELECT * FROM comparisons ORDER BY createdAt DESC")
        List<ComparisonRow> comparisons();

        @Query(
                "DELETE FROM motion_summaries WHERE endedAt < :cutoff OR id NOT IN (SELECT id FROM"
                        + " motion_summaries ORDER BY endedAt DESC LIMIT 5000)")
        void pruneMotion(long cutoff);

        @Query(
                "DELETE FROM comparisons WHERE createdAt < :cutoff OR id NOT IN (SELECT id FROM"
                        + " comparisons ORDER BY createdAt DESC LIMIT 100)")
        void pruneComparisons(long cutoff);

        @Query(
                "DELETE FROM notices WHERE state IN ('COMPLETED','SKIPPED','CANCELLED') AND"
                    + " (updatedAt < :cutoff OR id NOT IN (SELECT id FROM notices WHERE state IN"
                    + " ('COMPLETED','SKIPPED','CANCELLED') ORDER BY updatedAt DESC LIMIT 2000))")
        void pruneNotices(long cutoff);
    }
}
