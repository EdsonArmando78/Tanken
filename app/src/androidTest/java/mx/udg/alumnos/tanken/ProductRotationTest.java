package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ProductRotationTest {
    @Test
    public void breathingRotatesWithoutRestartingAndPausesWhenHidden() {
        AtomicReference<BreathingViewModel> original = new AtomicReference<>();
        try (ActivityScenario<BreathingActivity> scenario =
                ActivityScenario.launch(BreathingActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        BreathingViewModel model =
                                new ViewModelProvider(activity).get(BreathingViewModel.class);
                        original.set(model);
                        model.start();
                    });
            scenario.recreate();
            scenario.onActivity(
                    activity -> {
                        BreathingViewModel model =
                                new ViewModelProvider(activity).get(BreathingViewModel.class);
                        assertSame(original.get(), model);
                        assertEquals(
                                BreathingViewModel.Status.RUNNING, model.state().getValue().status);
                    });
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(
                    activity ->
                            assertEquals(
                                    BreathingViewModel.Status.PAUSED,
                                    original.get().state().getValue().status));
        }
    }

    @Test
    public void scheduleDraftAndSharedMotionSourceSurviveRotation() {
        AtomicReference<ScheduleViewModel> original = new AtomicReference<>();
        AtomicReference<MotionSource> motion = new AtomicReference<>();
        try (ActivityScenario<ScheduleActivity> scenario =
                ActivityScenario.launch(ScheduleActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        ScheduleViewModel model =
                                new ViewModelProvider(activity).get(ScheduleViewModel.class);
                        original.set(model);
                        model.edit(null);
                        model.name("Draft");
                        model.day(3);
                        model.selectedDay(1, true);
                        model.room("D-101");
                        model.time(true, "10:30");
                        motion.set(((TankenApplication) activity.getApplication()).motion());
                    });
            scenario.recreate();
            scenario.onActivity(
                    activity -> {
                        ScheduleViewModel model =
                                new ViewModelProvider(activity).get(ScheduleViewModel.class);
                        assertSame(original.get(), model);
                        assertEquals("Draft", model.name());
                        assertEquals(3, model.day());
                        assertEquals(5, model.days());
                        assertEquals("D-101", model.room());
                        assertEquals("10:30", model.time(true));
                        assertSame(
                                motion.get(),
                                ((TankenApplication) activity.getApplication()).motion());
                        model.cancel();
                    });
        }
    }
}
