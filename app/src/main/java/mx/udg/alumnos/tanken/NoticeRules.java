package mx.udg.alumnos.tanken;

/** Pure transition rules; responses describe user declarations, not measured habits. */
public final class NoticeRules {
    private NoticeRules() {}

    public enum State {
        SCHEDULED,
        SNOOZED,
        ISSUED,
        COMPLETED,
        SKIPPED,
        CANCELLED
    }

    public enum Reason {
        NONE,
        CLASS,
        USER,
        SILENCE,
        EXPIRED,
        DISABLED,
        SETTINGS_CHANGED,
        SCHEDULE_CHANGED,
        MOVEMENT
    }

    public static boolean terminal(State s) {
        return s == State.COMPLETED || s == State.SKIPPED || s == State.CANCELLED;
    }

    public static boolean allows(State from, State to, Reason reason, long at, long next) {
        if (terminal(from) || to == State.SCHEDULED) return false;
        if (to == State.SNOOZED)
            return (reason == Reason.CLASS || reason == Reason.USER || reason == Reason.SILENCE)
                    && next > at
                    && (reason != Reason.USER || from == State.ISSUED);
        if (to == State.COMPLETED || to == State.SKIPPED) return from == State.ISSUED;
        if (to == State.ISSUED) return from == State.SCHEDULED || from == State.SNOOZED;
        return to == State.CANCELLED;
    }
}
