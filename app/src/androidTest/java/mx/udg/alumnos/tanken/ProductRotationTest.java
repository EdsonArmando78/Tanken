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
                        assertEquals("10:30", model.time(true));
                        assertSame(
                                motion.get(),
                                ((TankenApplication) activity.getApplication()).motion());
                        model.cancel();
                    });
        }
    }
}
