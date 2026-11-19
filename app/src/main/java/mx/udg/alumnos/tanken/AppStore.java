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
    private final ReminderCoordinator reminders;
    private final CatalogRepository catalog;
    private volatile long dataGeneration;
    private final MutableLiveData<Boolean> notificationAsked = new MutableLiveData<>(false);
    private WellnessPreferences currentPreferences;
    private final Context context;

    AppStore(Context context) {
        this.context = context;
        db =
                Room.databaseBuilder(context, TankenDatabase.class, "tanken.db")
                        .addMigrations(
                                TankenDatabase.MIGRATION_1_2,
                                TankenDatabase.MIGRATION_2_3,
                                TankenDatabase.MIGRATION_3_4)
                        .openHelperFactory(new PreservingOpenHelperFactory())
                        .build();
        reminders = new ReminderCoordinator(context, db);
        catalog = new CatalogRepository(context, db, io);
        io.execute(
                () -> {
                    try {
                        String generation = db.records().metadata("dataGeneration");
                        dataGeneration = generation == null ? 0 : Long.parseLong(generation);
                        if ("1".equals(db.records().metadata("resetPending"))) performReset();
                        String warning = LegacyScheduleImport.run(db, context);
                        if (warning != null) error.postValue(warning);
                        try {
                            currentPreferences = readPreferences();
                            if ("1".equals(db.records().metadata("remindersDisabled"))) {
                                WellnessPreferences p = currentPreferences;
                                writePreferences(
                                        new WellnessPreferences(
                                                false,
                                                false,
                                                false,
                                                p.hydrationMinutes,
                                                p.breakMinutes,
                                                p.classLeadMinutes,
                                                p.dayMask,
                                                p.silenceStart,
                                                p.silenceEnd));
                            } else preferences.postValue(currentPreferences);
                        } catch (RuntimeException failure) {
                            report(failure);
                        }
                        notificationAsked.postValue(
                                context.getSharedPreferences(
                                                        "wellness_preferences",
                                                        Context.MODE_PRIVATE)
                                                .getBoolean("notificationAsked", false)
                                        || context.getSharedPreferences(
                                                        "TimersActivity", Context.MODE_PRIVATE)
                                                .getBoolean("notificationAsked", false));
                        catalog.initialize();
                        prune();
                        refresh();
                        if (currentPreferences != null)
                            reminders.reconcile(currentPreferences, snapshot, false);
                    } catch (java.io.IOException | RuntimeException failure) {
                        report(new IllegalStateException(failure));
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

    private void writePreferences(WellnessPreferences value) {
        boolean saved =
                context.getSharedPreferences("wellness_preferences", Context.MODE_PRIVATE)
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
        currentPreferences = value;
        preferences.postValue(value);
    }

    public void savePreferences(
            WellnessPreferences value, java.util.function.Consumer<String> done) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (currentPreferences == null)
                            throw new IllegalStateException("Preferences not readable");
                        writePreferences(value);
                        db.records()
                                .putMetadata(new TankenDatabase.Metadata("remindersDisabled", "0"));
                        reminders.reconcile(value, snapshot, true);
                        main.post(() -> done.accept(null));
                    } catch (RuntimeException failure) {
                        report(failure);
                        main.post(() -> done.accept(context.getString(R.string.storage_failed)));
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

    public void saveLessonGroup(
            String groupId,
            List<AcademicSchedule.Lesson> changes,
            java.util.function.Consumer<String> done) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        AcademicSchedule.Lesson conflict =
                                ScheduleEdits.replace(db, groupId, changes);
                        if (conflict != null) {
                            String message =
                                    context.getString(
                                            R.string.schedule_overlap_day,
                                            context.getResources()
                                                    .getStringArray(R.array.schedule_days)[
                                                    conflict.day.getValue() - 1],
                                            conflict.name);
                            main.post(() -> done.accept(message));
                            return;
                        }
                        refresh();
                        if (currentPreferences != null)
                            reminders.reconcile(currentPreferences, snapshot, false);
                        main.post(() -> done.accept(null));
                    } catch (IllegalArgumentException invalid) {
                        main.post(() -> done.accept(invalid.getMessage()));
                    } catch (RuntimeException failure) {
                        report(failure);
                        main.post(() -> done.accept(context.getString(R.string.storage_failed)));
                    }
                });
    }

    public void deleteLessonGroup(String id) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        db.records().deleteLessonGroup(id);
                        refresh();
                        if (currentPreferences != null)
                            reminders.reconcile(currentPreferences, snapshot, false);
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

    public void saveMotion(TankenDatabase.MotionRow row, long generation) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (generation != dataGeneration) return;
                        String cutoff = db.records().metadata("historyClearedAt");
                        if (cutoff != null && row.startedAt < Long.parseLong(cutoff)) return;
                        db.records().putMotion(row);
                        prune();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public void saveComparison(
            String id,
            List<AcademicSchedule.Lesson> input,
            WellnessSimulation.Comparison result,
            long executedAt,
            long generation) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (generation != dataGeneration) return;
                        String cutoff = db.records().metadata("historyClearedAt");
                        if (cutoff != null && executedAt < Long.parseLong(cutoff)) return;
                        TankenDatabase.ComparisonRow row = new TankenDatabase.ComparisonRow();
                        row.id = id;
                        row.createdAt = executedAt;
                        row.zone = result.start.getZone().getId();
                        row.inputAndResults = ComparisonCodec.encode(input, result);
                        db.records().putComparison(row);
                        prune();
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                });
    }

    public LiveData<List<TankenDatabase.NoticeRow>> openNotices() {
        return db.records().observeOpenNotices();
    }

    public LiveData<List<HistoryRow>> history(long from, long until, String category) {
        return db.records().history(from, until, category);
    }

    public void reconcileReminders() {
        handleReminderIntent(new android.content.Intent(), () -> {});
    }

    public void handleReminderIntent(android.content.Intent intent, Runnable finished) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (currentPreferences == null)
                            throw new IllegalStateException("Preferences unavailable");
                        if (ReminderReceiver.RESPOND.equals(intent.getAction())) {
                            reminders.respond(
                                    intent.getStringExtra("id"),
                                    intent.getLongExtra("revision", -1),
                                    NoticeRules.State.valueOf(intent.getStringExtra("response")),
                                    currentPreferences,
                                    snapshot);
                        } else reminders.reconcile(currentPreferences, snapshot, false);
                    } catch (RuntimeException failure) {
                        report(failure);
                    } finally {
                        finished.run();
                    }
                });
    }

    public void respondNotice(TankenDatabase.NoticeRow row, NoticeRules.State response) {
        android.content.Intent intent =
                new android.content.Intent()
                        .setAction(ReminderReceiver.RESPOND)
                        .putExtra("id", row.id)
                        .putExtra("revision", row.revision)
                        .putExtra("response", response.name());
        handleReminderIntent(intent, () -> {});
    }

    public void loadComparison(
            String id, java.util.function.Consumer<TankenDatabase.ComparisonRow> done) {
        io.execute(
                () -> {
                    TankenDatabase.ComparisonRow value = null;
                    try {
                        requireReady();
                        value = db.records().comparison(id);
                    } catch (RuntimeException failure) {
                        report(failure);
                    }
                    TankenDatabase.ComparisonRow result = value;
                    main.post(() -> done.accept(result));
                });
    }

    public void loadDetail(
            String category, String id, java.util.function.Consumer<RecordDetail> done) {
        io.execute(
                () -> {
                    RecordDetail value;
                    try {
                        requireReady();
                        value = RecordText.detail(context, db.records(), category, id);
                    } catch (RuntimeException failure) {
                        report(failure);
                        value =
                                new RecordDetail(
                                        context.getString(R.string.history_title),
                                        context.getString(R.string.record_unavailable),
                                        null,
                                        null);
                    }
                    RecordDetail result = value;
                    main.post(() -> done.accept(result));
                });
    }

    public void clearHistory(
            long from,
            long until,
            String category,
            boolean everything,
            java.util.function.Consumer<String> done) {
        io.execute(
                () -> {
                    try {
                        requireReady();
                        if (everything) {
                            // The durable gate also protects against a process death between
                            // preferences and deletion.
                            db.records()
                                    .putMetadata(
                                            new TankenDatabase.Metadata("remindersDisabled", "1"));
                            reminders.cancelAlarm();
                            ReminderNotifications.cancelAll(context);
                            if (currentPreferences == null)
                                throw new IllegalStateException("Preferences unavailable");
                            WellnessPreferences p = currentPreferences;
                            writePreferences(
                                    new WellnessPreferences(
                                            false,
                                            false,
                                            false,
                                            p.hydrationMinutes,
                                            p.breakMinutes,
                                            p.classLeadMinutes,
                                            p.dayMask,
                                            p.silenceStart,
                                            p.silenceEnd));
                        }
                        db.runInTransaction(
                                () -> {
                                    if (everything) {
                                        db.records()
                                                .putMetadata(
                                                        new TankenDatabase.Metadata(
                                                                "historyClearedAt",
                                                                Long.toString(
                                                                        System
                                                                                .currentTimeMillis())));
                                        db.records().deleteAllNotices();
                                        db.records().deleteMotion(Long.MIN_VALUE, Long.MAX_VALUE);
                                        db.records()
                                                .deleteComparisons(Long.MIN_VALUE, Long.MAX_VALUE);
                                    } else {
                                        if (category.equals("ALL") || category.equals("NOTICE"))
                                            db.records().deleteClosedNotices(from, until);
                                        if (category.equals("ALL") || category.equals("MOTION"))
                                            db.records().deleteMotion(from, until);
                                        if (category.equals("ALL") || category.equals("COMPARISON"))
                                            db.records().deleteComparisons(from, until);
                                    }
                                });
                        main.post(() -> done.accept(null));
                    } catch (RuntimeException failure) {
                        report(failure);
                        main.post(() -> done.accept(context.getString(R.string.storage_failed)));
                    }
                });
    }

    public CatalogRepository catalog() {
        return catalog;
    }

    public long generation() {
        return dataGeneration;
    }

    public LiveData<Boolean> notificationAsked() {
        return notificationAsked;
    }

    public void markNotificationAsked() {
        notificationAsked.setValue(true);
        io.execute(
                () -> {
                    if (!context.getSharedPreferences("wellness_preferences", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("notificationAsked", true)
                            .commit())
                        report(new IllegalStateException("Permission preference not saved"));
                });
    }

    public void resetAll(java.util.function.Consumer<String> done) {
        io.execute(
                () -> {
                    try {
                        // Validate the recovery catalog before changing anything.
                        catalog.bundled();
                        db.runInTransaction(
                                () -> {
                                    dataGeneration++;
                                    db.records()
                                            .putMetadata(
                                                    new TankenDatabase.Metadata(
                                                            "dataGeneration",
                                                            Long.toString(dataGeneration)));
                                    db.records()
                                            .putMetadata(
                                                    new TankenDatabase.Metadata(
                                                            "resetPending", "1"));
                                    db.records()
                                            .putMetadata(
                                                    new TankenDatabase.Metadata(
                                                            "remindersDisabled", "1"));
                                });
                        reminders.cancelAlarm();
                        androidx.core.app.NotificationManagerCompat.from(context).cancelAll();
                        MotionSource motion =
                                ((TankenApplication) context.getApplicationContext()).motion();
                        motion.prepareReset(
                                () ->
                                        io.execute(
                                                () -> {
                                                    try {
                                                        performReset();
                                                        catalog.initialize();
                                                        refresh();
                                                        error.postValue("");
                                                        motion.finishReset(
                                                                () ->
                                                                        main.post(
                                                                                () ->
                                                                                        done.accept(
                                                                                                null)));
                                                    } catch (java.io.IOException
                                                            | RuntimeException failure) {
                                                        report(new IllegalStateException(failure));
                                                        main.post(
                                                                () ->
                                                                        done.accept(
                                                                                context.getString(
                                                                                        R.string
                                                                                                .reset_failed)));
                                                    }
                                                }));
                    } catch (java.io.IOException | RuntimeException failure) {
                        report(new IllegalStateException(failure));
                        main.post(() -> done.accept(context.getString(R.string.reset_failed)));
                    }
                });
    }

    private void performReset() throws java.io.IOException {
        Catalog bundled = catalog.bundled();
        reminders.cancelAlarm();
        androidx.core.app.NotificationManagerCompat.from(context).cancelAll();
        for (String file :
                new String[] {
                    "academic_schedule",
                    "wellness_preferences",
                    "TimersActivity",
                    BreathingViewModel.PREFERENCES,
                    OnboardingState.PREFERENCES
                })
            if (!context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().commit())
                throw new IllegalStateException("Unable to clear preferences");
        writePreferences(WellnessPreferences.defaults());
        ResetData.clear(db, bundled, System.currentTimeMillis());
        notificationAsked.postValue(false);
    }
}
