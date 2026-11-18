package mx.udg.alumnos.tanken;

import android.content.Context;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class RecordText {
    private RecordText() {}

    static String date(Context c, long at) {
        return at <= 0
                ? c.getString(R.string.time_missing)
                : Instant.ofEpochMilli(at)
                        .atZone(ZoneId.systemDefault())
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm:ss", Locale.getDefault()));
    }

    static String kind(Context c, String kind) {
        int id;
        switch (kind) {
            case "HYDRATION":
                id = R.string.wellness_hydration;
                break;
            case "ACTIVE_BREAK":
                id = R.string.wellness_break;
                break;
            case "CLASS":
                id = R.string.notice_class;
                break;
            case "MOTION":
                id = R.string.motion_title;
                break;
            case "COMPARISON":
                id = R.string.wellness_title;
                break;
            default:
                id = R.string.reminders_title;
        }
        return c.getString(id);
    }

    static String state(Context c, String state) {
        int id;
        switch (state) {
            case "SCHEDULED":
                id = R.string.notice_scheduled;
                break;
            case "SNOOZED":
                id = R.string.notice_snoozed;
                break;
            case "ISSUED":
                id = R.string.notice_issued;
                break;
            case "COMPLETED":
                id = R.string.notice_done;
                break;
            case "SKIPPED":
                id = R.string.notice_skip;
                break;
            case "CANCELLED":
                id = R.string.notice_cancelled;
                break;
            default:
                id = R.string.notice_none;
        }
        return c.getString(id);
    }

    static String reason(Context c, String reason) {
        int id;
        switch (reason) {
            case "CLASS":
                id = R.string.reason_class;
                break;
            case "USER":
                id = R.string.reason_user;
                break;
            case "SILENCE":
                id = R.string.reason_silence;
                break;
            case "EXPIRED":
                id = R.string.reason_expired;
                break;
            case "DISABLED":
                id = R.string.reason_disabled;
                break;
            case "SETTINGS_CHANGED":
                id = R.string.reason_settings;
                break;
            case "SCHEDULE_CHANGED":
                id = R.string.reason_schedule;
                break;
            case "MOVEMENT":
                id = R.string.reason_movement;
                break;
            default:
                id = R.string.reason_none;
        }
        return c.getString(id);
    }

    static String origin(Context c, String origin) {
        return c.getString(
                origin.equals("SIMULATED")
                        ? R.string.origin_simulated
                        : origin.equals("REAL") ? R.string.origin_real : R.string.origin_motion);
    }

    static String publication(Context c, String publication) {
        return c.getString(
                publication.startsWith("BLOCKED")
                        ? R.string.publication_blocked
                        : publication.equals("PUBLISHED")
                                ? R.string.publication_sent
                                : publication.equals("PENDING")
                                        ? R.string.publication_pending
                                        : R.string.publication_none);
    }

    static RecordDetail detail(
            Context c, TankenDatabase.Records records, String category, String id) {
        switch (category) {
            case "NOTICE":
                {
                    TankenDatabase.NoticeRow n = records.notice(id);
                    if (n == null) throw new IllegalArgumentException("Missing notice");
                    String body =
                            c.getString(
                                    R.string.notice_detail,
                                    state(c, n.state),
                                    origin(c, n.origin),
                                    date(c, n.originalDue),
                                    date(c, n.nextAttempt),
                                    date(c, n.receivedAt),
                                    date(c, n.emittedAt),
                                    date(c, n.answeredAt),
                                    reason(c, n.reason),
                                    state(c, n.response),
                                    publication(c, n.publication));
                    if (n.kind.equals("CLASS"))
                        body =
                                c.getString(
                                        R.string.class_detail,
                                        n.lessonName,
                                        date(c, n.targetStart),
                                        body);
                    StringBuilder transitions = new StringBuilder(body);
                    for (TankenDatabase.TransitionRow t : records.transitions(id))
                        transitions.append(
                                c.getString(
                                        R.string.notice_transition,
                                        date(c, t.at),
                                        state(c, t.state),
                                        reason(c, t.reason),
                                        date(c, t.nextAttempt)));
                    return new RecordDetail(kind(c, n.kind), transitions.toString(), n, null);
                }
            case "MOTION":
                {
                    TankenDatabase.MotionRow m = records.motion(id);
                    if (m == null) throw new IllegalArgumentException("Missing summary");
                    return new RecordDetail(
                            kind(c, "MOTION"),
                            c.getString(
                                    R.string.motion_detail,
                                    date(c, m.startedAt),
                                    date(c, m.endedAt),
                                    m.samples,
                                    m.observedMillis / 1000,
                                    m.peak),
                            null,
                            null);
                }
            case "COMPARISON":
                {
                    TankenDatabase.ComparisonRow row = records.comparison(id);
                    if (row == null) throw new IllegalArgumentException("Missing comparison");
                    return new RecordDetail(
                            kind(c, "COMPARISON"),
                            ComparisonReport.format(c, ComparisonCodec.read(row.inputAndResults)),
                            null,
                            id);
                }
            default:
                throw new IllegalArgumentException("Unknown category");
        }
    }
}
