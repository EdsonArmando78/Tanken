package mx.udg.alumnos.tanken;

import static mx.udg.alumnos.tanken.NoticeRules.Reason.*;
import static mx.udg.alumnos.tanken.NoticeRules.State.*;

import static org.junit.Assert.*;

import org.junit.Test;

public class NoticeRulesTest {
    @Test
    public void terminalStatesRejectEveryAction() {
        for (NoticeRules.State s : new NoticeRules.State[] {COMPLETED, SKIPPED, CANCELLED})
            for (NoticeRules.State target : NoticeRules.State.values())
                assertFalse(NoticeRules.allows(s, target, USER, 100, 200));
    }

    @Test
    public void userResponseRequiresIssuedNotice() {
        assertTrue(NoticeRules.allows(ISSUED, COMPLETED, USER, 100, 0));
        assertFalse(NoticeRules.allows(SCHEDULED, COMPLETED, USER, 100, 0));
        assertFalse(NoticeRules.allows(SNOOZED, SKIPPED, USER, 100, 0));
    }

    @Test
    public void classAndUserSnoozesHaveDifferentPreconditions() {
        assertTrue(NoticeRules.allows(SCHEDULED, SNOOZED, CLASS, 100, 200));
        assertFalse(NoticeRules.allows(SCHEDULED, SNOOZED, USER, 100, 200));
        assertTrue(NoticeRules.allows(ISSUED, SNOOZED, USER, 100, 200));
    }

    @Test
    public void snoozeRequiresReasonAndFutureAttempt() {
        assertFalse(NoticeRules.allows(ISSUED, SNOOZED, NONE, 100, 200));
        assertFalse(NoticeRules.allows(ISSUED, SNOOZED, USER, 100, 100));
    }

    @Test
    public void repeatedIssueIsRejected() {
        assertTrue(NoticeRules.allows(SCHEDULED, ISSUED, NONE, 100, 0));
        assertTrue(NoticeRules.allows(SNOOZED, ISSUED, NONE, 100, 0));
        assertFalse(NoticeRules.allows(ISSUED, ISSUED, NONE, 100, 0));
    }
}
