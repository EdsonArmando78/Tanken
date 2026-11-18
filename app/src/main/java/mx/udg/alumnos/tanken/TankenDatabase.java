package mx.udg.alumnos.tanken;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.*;

import java.util.List;

/** Schema v1. All access is mediated by AppStore, never by a screen. */
@Database(
        entities = {
            TankenDatabase.LessonRow.class,
            TankenDatabase.NoticeRow.class,
            TankenDatabase.TransitionRow.class,
            TankenDatabase.MotionRow.class,
            TankenDatabase.ComparisonRow.class,
            TankenDatabase.Metadata.class
        },
        version = 1,
        exportSchema = true)
public abstract class TankenDatabase extends RoomDatabase {
    public abstract Records records();

    @Entity(tableName = "lessons", indices = @Index(value = {"day", "start"}))
    public static class LessonRow {
        @PrimaryKey @NonNull public String id = "";
        @NonNull public String name = "";
        public int day;
        @NonNull public String start = "", end = "";
        public long updatedAt;

        public LessonRow() {}

        @Ignore
        public LessonRow(AcademicSchedule.Lesson l) {
            id = l.id;
            name = l.name;
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
                    java.time.LocalTime.parse(end));
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
        @Query("SELECT * FROM lessons ORDER BY day, start")
        LiveData<List<LessonRow>> observeLessons();

        @Query("SELECT * FROM lessons ORDER BY day, start")
        List<LessonRow> lessons();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void putLesson(LessonRow row);

        @Query("DELETE FROM lessons WHERE id = :id")
        void deleteLesson(String id);

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
