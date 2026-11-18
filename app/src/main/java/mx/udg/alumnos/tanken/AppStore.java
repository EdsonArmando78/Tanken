package mx.udg.alumnos.tanken;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import org.json.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The single process writer. Storage failures never trigger destructive recovery. */
public final class AppStore {
    public static AppStore get(Context context) {
        return ((TankenApplication) context.getApplicationContext()).store();
    }

    final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<AcademicSchedule.Lesson>> lessons = new MutableLiveData<>();
    private final MutableLiveData<WellnessPreferences> preferences = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>("");
    private volatile List<AcademicSchedule.Lesson> snapshot = Collections.emptyList();
    private volatile boolean ready;
    private final TankenDatabase db;
    private final Context context;

    AppStore(Context context) {
        this.context = context;
        db =
                Room.databaseBuilder(context, TankenDatabase.class, "tanken.db")
                        .openHelperFactory(new PreservingOpenHelperFactory())
                        .build();
        io.execute(
                () -> {
                    try {
                        String warning = LegacyScheduleImport.run(db, context);
                        if (warning != null) error.postValue(warning);
                        try {
                            preferences.postValue(readPreferences());
                        } catch (RuntimeException failure) {
                            report(failure);
                        }
                        prune();
                        refresh();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public LiveData<WellnessPreferences> preferences() {
        return preferences;
    }

    private WellnessPreferences readPreferences() {
        android.content.SharedPreferences p =
                context.getSharedPreferences("wellness_preferences", Context.MODE_PRIVATE);
        if (p.getInt("formatVersion", 1) != WellnessPreferences.FORMAT_VERSION)
            throw new IllegalStateException("Unknown preferences format");
        return new WellnessPreferences(
                p.getBoolean("hydrationEnabled", false),
                p.getBoolean("breaksEnabled", false),
                p.getBoolean("classesEnabled", false),
                p.getInt("hydrationMinutes", 60),
                p.getInt("breakMinutes", 60),
                p.getInt("classLeadMinutes", 10),
                p.getInt("dayMask", 31),
                LocalTime.parse(p.getString("silenceStart", "22:00")),
                LocalTime.parse(p.getString("silenceEnd", "07:00")));
    }

    public void savePreferences(WellnessPreferences value) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (preferences.getValue() == null)
                            throw new IllegalStateException("Preferences not readable");
                        boolean saved =
                                context.getSharedPreferences(
                                                "wellness_preferences", Context.MODE_PRIVATE)
                                        .edit()
                                        .putInt("formatVersion", WellnessPreferences.FORMAT_VERSION)
                                        .putBoolean("hydrationEnabled", value.hydrationEnabled)
                                        .putBoolean("breaksEnabled", value.breaksEnabled)
                                        .putBoolean("classesEnabled", value.classesEnabled)
                                        .putInt("hydrationMinutes", value.hydrationMinutes)
                                        .putInt("breakMinutes", value.breakMinutes)
                                        .putInt("classLeadMinutes", value.classLeadMinutes)
                                        .putInt("dayMask", value.dayMask)
                                        .putString("silenceStart", value.silenceStart.toString())
                                        .putString("silenceEnd", value.silenceEnd.toString())
                                        .commit();
                        if (!saved) throw new IllegalStateException("Preferences write failed");
                        preferences.postValue(value);
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public LiveData<List<AcademicSchedule.Lesson>> lessons() {
        return lessons;
    }

    public LiveData<String> error() {
        return error;
    }

    public List<AcademicSchedule.Lesson> snapshot() {
        return snapshot;
    }

    public boolean ready() {
        return ready;
    }

    private void refresh() {
        List<AcademicSchedule.Lesson> result = new ArrayList<>();
        for (TankenDatabase.LessonRow row : db.records().lessons()) result.add(row.lesson());
        snapshot = Collections.unmodifiableList(result);
        ready = true;
        lessons.postValue(snapshot);
    }

    private void report(RuntimeException failure) {
        error.postValue(context.getString(R.string.storage_failed));
        android.util.Log.e("TankenStore", "Storage failed; files preserved", failure);
    }

    public void saveLesson(
            AcademicSchedule.Lesson lesson, java.util.function.Consumer<String> done) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        db.runInTransaction(
                                () -> {
                                    List<AcademicSchedule.Lesson> existing = new ArrayList<>();
                                    for (TankenDatabase.LessonRow row : db.records().lessons())
                                        existing.add(row.lesson());
                                    AcademicSchedule.Lesson conflict =
                                            ScheduleValidator.conflict(existing, lesson);
                                    if (conflict != null)
                                        throw new IllegalArgumentException(
                                                context.getString(
                                                        R.string.schedule_overlap, conflict.name));
                                    db.records().putLesson(new TankenDatabase.LessonRow(lesson));
                                });
                        refresh();
                        main.post(() -> done.accept(null));
                    } catch (IllegalArgumentException invalid) {
                        main.post(() -> done.accept(invalid.getMessage()));
                    } catch (RuntimeException failure) {
                        report(failure);
                        main.post(() -> done.accept(context.getString(R.string.storage_failed)));
                    }
                });
    }

    public void deleteLesson(String id) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        db.records().deleteLesson(id);
                        refresh();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    private void requireReady() {
        if (!ready) throw new IllegalStateException("Storage not ready");
    }

    private void prune() {
        long cutoff = System.currentTimeMillis() - Duration.ofDays(30).toMillis();
        db.runInTransaction(
                () -> {
                    db.records().pruneMotion(cutoff);
                    db.records().pruneComparisons(cutoff);
                    db.records().pruneNotices(cutoff);
                });
    }

    public void saveMotion(TankenDatabase.MotionRow row) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        db.records().putMotion(row);
                        prune();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public void saveComparison(
            String id, List<AcademicSchedule.Lesson> input, WellnessSimulation.Comparison result) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        TankenDatabase.ComparisonRow row = new TankenDatabase.ComparisonRow();
                        row.id = id;
                        row.createdAt = System.currentTimeMillis();
                        row.zone = result.start.getZone().getId();
                        row.inputAndResults = ComparisonCodec.encode(input, result);
                        db.records().putComparison(row);
                        prune();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public void createNotice(TankenDatabase.NoticeRow row) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (row.id.isEmpty()
                                || row.kind.isEmpty()
                                || row.originalDue <= 0
                                || !row.state.equals("SCHEDULED"))
                            throw new IllegalArgumentException("Invalid notice");
                        db.records().insertNotice(row);
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public void respond(
            String id,
            String actionId,
            NoticeRules.State next,
            NoticeRules.Reason reason,
            long attempt,
            java.util.function.Consumer<Boolean> done) {
        io.execute(
                () -> {
                    boolean[] changed = {false};
                    try {
                        requireReady();
                        changed[0] =
                                NoticeTransitions.apply(
                                        db,
                                        id,
                                        actionId,
                                        next,
                                        reason,
                                        System.currentTimeMillis(),
                                        attempt);
                        prune();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                    main.post(() -> done.accept(changed[0]));
                });
    }
}
