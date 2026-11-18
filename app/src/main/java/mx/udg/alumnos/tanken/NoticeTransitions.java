package mx.udg.alumnos.tanken;

import java.util.UUID;

/** One transaction for state, response, and action deduplication. */
final class NoticeTransitions {
    private NoticeTransitions() {}

    static boolean apply(
            TankenDatabase db,
            String id,
            String actionId,
            NoticeRules.State next,
            NoticeRules.Reason reason,
            long at,
            long attempt) {
        boolean[] changed = {false};
        db.runInTransaction(
                () -> {
                    TankenDatabase.NoticeRow row = db.records().notice(id);
                    if (row == null
                            || actionId.isEmpty()
                            || db.records().actionCount(id, actionId) != 0
                            || !NoticeRules.allows(
                                    NoticeRules.State.valueOf(row.state),
                                    next,
                                    reason,
                                    at,
                                    attempt)) return;
                    row.state = next.name();
                    row.reason = reason.name();
                    row.updatedAt = at;
                    row.nextAttempt = next == NoticeRules.State.SNOOZED ? attempt : 0;
                    if (next == NoticeRules.State.COMPLETED
                            || next == NoticeRules.State.SKIPPED
                            || reason == NoticeRules.Reason.USER) row.response = next.name();
                    TankenDatabase.TransitionRow transition = new TankenDatabase.TransitionRow();
                    transition.id = UUID.randomUUID().toString();
                    transition.noticeId = id;
                    transition.actionId = actionId;
                    transition.at = at;
                    transition.state = row.state;
                    transition.reason = row.reason;
                    transition.nextAttempt = row.nextAttempt;
                    db.records().updateNotice(row);
                    db.records().transition(transition);
                    changed[0] = true;
                });
        return changed[0];
    }
}
