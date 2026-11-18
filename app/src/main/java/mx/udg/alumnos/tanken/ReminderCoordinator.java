package mx.udg.alumnos.tanken;

import android.app.*;
import android.content.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Called only on AppStore's serial executor. Room is authoritative; alarms are recoverable. */
final class ReminderCoordinator {
    private final Context context;
    private final TankenDatabase db;

    ReminderCoordinator(Context context, TankenDatabase db) {
        this.context = context;
        this.db = db;
    }

    private PendingIntent alarmIntent() {
        return PendingIntent.getBroadcast(
                context,
                0,
                new Intent(context, ReminderReceiver.class).setAction(ReminderReceiver.DUE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    void cancelAlarm() {
        context.getSystemService(AlarmManager.class).cancel(alarmIntent());
    }

    void reconcile(
            WellnessPreferences prefs,
            List<AcademicSchedule.Lesson> lessons,
            boolean settingsChanged) {
        ReminderNotifications.createChannel(context);
        ZonedDateTime now = ZonedDateTime.now();
        String signature =
                prefs.hydrationEnabled
                        + ":"
                        + prefs.breaksEnabled
                        + ":"
                        + prefs.classesEnabled
                        + ":"
                        + prefs.hydrationMinutes
                        + ":"
                        + prefs.breakMinutes
                        + ":"
                        + prefs.classLeadMinutes
                        + ":"
                        + prefs.dayMask
                        + ":"
                        + prefs.silenceStart
                        + ":"
                        + prefs.silenceEnd;
        String previous = db.records().metadata("reminderConfig");
        if (settingsChanged || (previous != null && !previous.equals(signature))) {
            long generation = generation() + 1;
            db.records()
                    .putMetadata(
                            new TankenDatabase.Metadata(
                                    "reminderGeneration", Long.toString(generation)));
            for (TankenDatabase.NoticeRow row : db.records().openNotices())
                cancel(row, "SETTINGS_CHANGED", now);
        }
        db.records().putMetadata(new TankenDatabase.Metadata("reminderConfig", signature));
        if ("1".equals(db.records().metadata("remindersDisabled"))) {
            cancelAlarm();
            ReminderNotifications.cancelAll(context);
            return;
        }
        processOpen(prefs, lessons, now);
        List<TankenDatabase.NoticeRow> open = db.records().openNotices();
        for (String kind : new String[] {"HYDRATION", "ACTIVE_BREAK"}) {
            if (ReminderPolicy.enabled(kind, prefs)
                    && open.stream().noneMatch(row -> row.kind.equals(kind))) {
                int interval =
                        kind.equals("HYDRATION") ? prefs.hydrationMinutes : prefs.breakMinutes;
                ZonedDateTime due = ReminderPolicy.nextAllowed(now.plusMinutes(interval), prefs);
                TankenDatabase.NoticeRow row = notice(UUID.randomUUID().toString(), kind, due, now);
                row.expiresAt =
                        due.toLocalDate()
                                .plusDays(1)
                                .atStartOfDay(due.getZone())
                                .toInstant()
                                .toEpochMilli();
                db.records().insertNotice(row);
            }
        }
        if (prefs.classesEnabled && open.stream().noneMatch(row -> row.kind.equals("CLASS")))
            createNextClass(prefs, lessons, now);
        processOpen(prefs, lessons, now);
        long next = Long.MAX_VALUE;
        for (TankenDatabase.NoticeRow row : db.records().openNotices())
            next = Math.min(next, row.state.equals("ISSUED") ? row.expiresAt : row.nextAttempt);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (next == Long.MAX_VALUE) cancelAlarm();
        else
            alarms.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    Math.max(System.currentTimeMillis() + 1000, next),
                    alarmIntent());
    }

    private void processOpen(
            WellnessPreferences prefs, List<AcademicSchedule.Lesson> lessons, ZonedDateTime now) {
        long at = now.toInstant().toEpochMilli();
        for (TankenDatabase.NoticeRow row : db.records().openNotices()) {
            if (!ReminderPolicy.enabled(row.kind, prefs)) {
                cancel(row, "DISABLED", now);
                continue;
            }
            if (row.expiresAt == 0) {
                row.expiresAt =
                        Instant.ofEpochMilli(row.originalDue)
                                .atZone(now.getZone())
                                .toLocalDate()
                                .plusDays(1)
                                .atStartOfDay(now.getZone())
                                .toInstant()
                                .toEpochMilli();
                db.records().updateNotice(row);
            }
            if (row.expiresAt <= at) {
                cancel(row, "EXPIRED", now);
                continue;
            }
            if (row.kind.equals("CLASS") && !sameOccurrence(row, lessons, now)) {
                cancel(row, "SCHEDULE_CHANGED", now);
                continue;
            }
            if (row.state.equals("ISSUED") && row.publication.equals("PUBLISHED")) continue;
            if (row.nextAttempt <= at && !row.state.equals("ISSUED")) {
                row.receivedAt = at;
                db.records().updateNotice(row);
            }
            boolean contextual = row.reason.equals("CLASS") || row.reason.equals("SILENCE");
            long requested =
                    Math.max(row.userNotBefore, contextual ? row.originalDue : row.nextAttempt);
            // Evaluate context at the deadline, not while creating a future interval.
            if (!row.state.equals("ISSUED") && requested > at) continue;
            ZonedDateTime candidate =
                    Instant.ofEpochMilli(Math.max(at, requested)).atZone(now.getZone());
            ZonedDateTime allowed = ReminderPolicy.nextAllowed(candidate, prefs);
            ZonedDateTime target =
                    row.kind.equals("CLASS")
                            ? allowed
                            : ReminderPolicy.wellnessTime(
                                    lessons,
                                    allowed,
                                    prefs,
                                    Instant.ofEpochMilli(row.expiresAt).atZone(now.getZone()));
            long attempt = target.toInstant().toEpochMilli();
            if (attempt >= row.expiresAt) {
                cancel(row, "EXPIRED", now);
                continue;
            }
            if (attempt > at) {
                if (attempt != row.nextAttempt) {
                    NoticeRules.Reason reason =
                            allowed.isAfter(candidate)
                                    ? NoticeRules.Reason.SILENCE
                                    : NoticeRules.Reason.CLASS;
                    // A future interval that needs no deferral remains SCHEDULED.
                    if (target.isAfter(candidate) || contextual) {
                        NoticeTransitions.apply(
                                db,
                                row.id,
                                UUID.randomUUID().toString(),
                                NoticeRules.State.SNOOZED,
                                reason,
                                at,
                                attempt);
                        ReminderNotifications.cancel(context, row.id);
                    }
                }
                continue;
            }
            MotionSource motion = ((TankenApplication) context.getApplicationContext()).motion();
            if (row.kind.equals("ACTIVE_BREAK") && motion != null && motion.recentlyMoving()) {
                cancel(row, "MOVEMENT", now);
                continue;
            }
            if (row.state.equals("ISSUED")) {
                publish(row, at);
                continue;
            }
            row.receivedAt = at;
            row.revision++;
            row.publication = "PENDING";
            row.emittedAt = 0;
            final TankenDatabase.NoticeRow pending = row;
            // State and pending publication commit together before touching NotificationManager.
            db.runInTransaction(
                    () -> {
                        db.records().updateNotice(pending);
                        NoticeTransitions.apply(
                                db,
                                pending.id,
                                "issue:" + pending.revision,
                                NoticeRules.State.ISSUED,
                                NoticeRules.Reason.NONE,
                                at,
                                0);
                    });
            publish(db.records().notice(row.id), at);
        }
    }

    private void publish(TankenDatabase.NoticeRow row, long at) {
        row.publication = ReminderNotifications.publish(context, row);
        if (row.publication.equals("PUBLISHED") && row.emittedAt == 0) row.emittedAt = at;
        db.records().updateNotice(row);
    }

    void respond(
            String id,
            long revision,
            NoticeRules.State response,
            WellnessPreferences prefs,
            List<AcademicSchedule.Lesson> lessons) {
        TankenDatabase.NoticeRow row = db.records().notice(id);
        if (row == null || row.revision != revision || !row.state.equals("ISSUED")) return;
        long now = System.currentTimeMillis();
        if (row.expiresAt <= now
                || (response == NoticeRules.State.SNOOZED && now + 600_000 >= row.expiresAt)) {
            cancel(row, "EXPIRED", ZonedDateTime.now());
        } else if (response == NoticeRules.State.COMPLETED
                || response == NoticeRules.State.SKIPPED
                || response == NoticeRules.State.SNOOZED) {
            boolean changed =
                    NoticeTransitions.apply(
                            db,
                            id,
                            "user:" + revision,
                            response,
                            NoticeRules.Reason.USER,
                            now,
                            response == NoticeRules.State.SNOOZED ? now + 600_000 : 0);
            if (changed) {
                ReminderNotifications.cancel(context, id);
            }
        }
        reconcile(prefs, lessons, false);
    }

    private void cancel(TankenDatabase.NoticeRow row, String reason, ZonedDateTime now) {
        NoticeTransitions.apply(
                db,
                row.id,
                "cancel:" + UUID.randomUUID(),
                NoticeRules.State.CANCELLED,
                NoticeRules.Reason.valueOf(reason),
                now.toInstant().toEpochMilli(),
                0);
        ReminderNotifications.cancel(context, row.id);
    }

    private boolean sameOccurrence(
            TankenDatabase.NoticeRow row,
            List<AcademicSchedule.Lesson> lessons,
            ZonedDateTime now) {
        for (AcademicSchedule.Lesson lesson : lessons)
            if (lesson.id.equals(row.lessonId)) {
                AcademicSchedule.Occurrence next =
                        AcademicSchedule.at(Collections.singletonList(lesson), now).next;
                boolean same =
                        next != null && next.start.toInstant().toEpochMilli() == row.targetStart;
                if (same && !row.lessonName.equals(lesson.name)) {
                    row.lessonName = lesson.name;
                    db.records().updateNotice(row);
                    if (row.publication.equals("PUBLISHED"))
                        publish(row, System.currentTimeMillis());
                }
                return same;
            }
        return false;
    }

    private long generation() {
        String value = db.records().metadata("reminderGeneration");
        return value == null ? 0 : Long.parseLong(value);
    }

    private void createNextClass(
            WellnessPreferences prefs, List<AcademicSchedule.Lesson> lessons, ZonedDateTime now) {
        TankenDatabase.NoticeRow best = null;
        long generation = generation();
        for (int day = 0; day <= 7; day++)
            for (AcademicSchedule.Lesson lesson : lessons) {
                LocalDate date = now.toLocalDate().plusDays(day);
                if (date.getDayOfWeek() != lesson.day) continue;
                ZonedDateTime start = date.atTime(lesson.start).atZone(now.getZone());
                if (!start.isAfter(now)) continue;
                ZonedDateTime due = start.minusMinutes(prefs.classLeadMinutes);
                ZonedDateTime allowed =
                        ReminderPolicy.nextAllowed(due.isBefore(now) ? now : due, prefs);
                if (!allowed.isBefore(start)) continue;
                String id =
                        UUID.nameUUIDFromBytes(
                                        (lesson.id
                                                        + ":"
                                                        + start.toInstant().toEpochMilli()
                                                        + ":"
                                                        + generation)
                                                .getBytes(StandardCharsets.UTF_8))
                                .toString();
                if (db.records().notice(id) != null) continue;
                if (best != null && best.targetStart <= start.toInstant().toEpochMilli()) continue;
                best = notice(id, "CLASS", due, now);
                best.lessonId = lesson.id;
                best.lessonName = lesson.name;
                best.targetStart = best.expiresAt = start.toInstant().toEpochMilli();
            }
        if (best != null) db.records().insertNotice(best);
    }

    private TankenDatabase.NoticeRow notice(
            String id, String kind, ZonedDateTime due, ZonedDateTime now) {
        TankenDatabase.NoticeRow row = new TankenDatabase.NoticeRow();
        row.id = id;
        row.kind = kind;
        row.createdAt = row.updatedAt = now.toInstant().toEpochMilli();
        row.originalDue = row.nextAttempt = due.toInstant().toEpochMilli();
        row.zone = due.getZone().getId();
        return row;
    }
}
