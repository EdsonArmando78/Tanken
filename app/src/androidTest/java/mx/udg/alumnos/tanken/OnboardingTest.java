package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/** Does not change the app's welcome flag or request/revoke system permissions. */
@RunWith(AndroidJUnit4.class)
public class OnboardingTest {
    @Test
    public void completionIsIndependentOfPermissionsAndPersistsInPreferences() {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "test-welcome-" + UUID.randomUUID();
        Context isolated =
                new ContextWrapper(base) {
                    @Override
                    public SharedPreferences getSharedPreferences(String ignored, int mode) {
                        return base.getSharedPreferences(name, mode);
                    }
                };
        try {
            assertFalse(OnboardingState.completed(isolated));
            OnboardingState.complete(isolated);
            assertTrue(OnboardingState.completed(isolated));
            assertFalse(isolated.getSharedPreferences(name, 0).contains("notificationAsked"));
        } finally {
            base.deleteSharedPreferences(name);
        }
    }

    @Test
    public void replayRetainsPageOnRotationAndDoesNotChangeCompletionFlag() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean before = OnboardingState.completed(context);
        Intent replay =
                new Intent(context, OnboardingActivity.class)
                        .putExtra(OnboardingActivity.REPLAY, true);
        try (ActivityScenario<OnboardingActivity> scenario = ActivityScenario.launch(replay)) {
            scenario.onActivity(
                    activity -> {
                        ViewPager2 pager = activity.findViewById(R.id.pages);
                        assertEquals(OnboardingAdapter.COUNT, pager.getAdapter().getItemCount());
                        pager.setCurrentItem(3, false);
                    });
            scenario.recreate();
            scenario.onActivity(
                    activity -> {
                        ViewPager2 pager = activity.findViewById(R.id.pages);
                        assertEquals(3, pager.getCurrentItem());
                        activity.findViewById(R.id.previous).performClick();
                        assertEquals(2, pager.getCurrentItem());
                        activity.findViewById(R.id.skip).performClick();
                    });
        }
        assertEquals(before, OnboardingState.completed(context));
    }
}
