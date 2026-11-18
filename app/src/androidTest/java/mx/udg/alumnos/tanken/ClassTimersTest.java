package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClassTimersTest {
    private void main(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private ClassTimersViewModel model() {
        Application app = (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getApplicationContext();
        return new ClassTimersViewModel(app);
    }

    @Test public void onlyFourRunAndCancellationStartsNextWithoutChangingOtherIds() {
        main(() -> {
            ClassTimersViewModel model = model();
            try {
                for (int i = 1; i <= 6; i++) model.add("Clase " + i, 60 + i);
                List<ClassTimersViewModel.Row> rows = model.getRows().getValue();
                assertEquals(6, rows.size());
                for (int i = 0; i < 4; i++) assertEquals(ClassTimersViewModel.Status.RUNNING, rows.get(i).status);
                assertEquals(ClassTimersViewModel.Status.WAITING, rows.get(4).status);
                model.cancel(6); // Cancelling a queued timer never consumes a worker.
                model.cancel(2);
                rows = model.getRows().getValue();
                assertEquals(ClassTimersViewModel.Status.CANCELLED, rows.get(1).status);
                assertEquals(ClassTimersViewModel.Status.RUNNING, rows.get(4).status);
                assertEquals(ClassTimersViewModel.Status.CANCELLED, rows.get(5).status);
                assertEquals(65, rows.get(4).secondsLeft);
                model.cancel(2); // Idempotent cancellation.
                model.clearHistory();
                rows = model.getRows().getValue();
                assertEquals(4, rows.size());
                assertEquals(1, rows.get(0).id);
                assertEquals(5, rows.get(3).id);
            } finally { model.onCleared(); }
        });
    }

    @Test public void completionAndCancellationAreIndependentAndReleaseWorkers() throws Exception {
        AtomicReference<ClassTimersViewModel> ref = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Observer<List<ClassTimersViewModel.Row>> observer = rows -> {
            if (rows.size() == 6 && rows.get(0).status == ClassTimersViewModel.Status.FINISHED
                    && rows.get(5).status == ClassTimersViewModel.Status.RUNNING) completed.countDown();
        };
        main(() -> {
            ClassTimersViewModel model = model();
            ref.set(model);
            model.getRows().observeForever(observer);
            model.add("Aviso corto", 1);
            model.add("Cancelar", 60);
            model.add("Largo A", 60);
            model.add("Largo B", 60);
            model.add("En espera", 60);
            model.add("Espera al aviso corto", 60);
            model.cancel(2); // Starts #5 immediately; #1 must still finish independently.
        });
        try {
            assertTrue("El temporizador corto debe finalizar", completed.await(5, TimeUnit.SECONDS));
            main(() -> {
                List<ClassTimersViewModel.Row> rows = ref.get().getRows().getValue();
                assertEquals(0, rows.get(0).secondsLeft);
                assertEquals(ClassTimersViewModel.Status.CANCELLED, rows.get(1).status);
                assertEquals(ClassTimersViewModel.Status.RUNNING, rows.get(2).status);
                assertEquals("Largo A", rows.get(2).name);
            });
        } finally {
            main(() -> {
                ref.get().getRows().removeObserver(observer);
                ref.get().onCleared();
            });
        }
    }

    @Test public void rotationRetainsTimersAndFinalCloseClearsWorkers() {
        AtomicReference<ClassTimersViewModel> ref = new AtomicReference<>();
        try (ActivityScenario<TimersActivity> scenario = ActivityScenario.launch(TimersActivity.class)) {
            scenario.onActivity(activity -> {
                ClassTimersViewModel model = new ViewModelProvider(activity).get(ClassTimersViewModel.class);
                ref.set(model);
                model.add("Clase tras rotación", 60);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                ClassTimersViewModel model = new ViewModelProvider(activity).get(ClassTimersViewModel.class);
                assertSame(ref.get(), model);
                assertEquals(ClassTimersViewModel.Status.RUNNING, model.getRows().getValue().get(0).status);
            });
        }
        main(() -> {
            int count = ref.get().getRows().getValue().size();
            ref.get().add("No debe iniciarse tras cerrar", 10);
            assertEquals(count, ref.get().getRows().getValue().size());
        });
    }

    @Test public void rejectsInvalidDurationAndEmptyName() {
        main(() -> {
            ClassTimersViewModel model = model();
            try {
                for (long duration : new long[] {0, -1, 86401, Long.MAX_VALUE}) {
                    assertThrows(IllegalArgumentException.class, () -> model.add("Clase", duration));
                }
                assertThrows(IllegalArgumentException.class, () -> model.add("  ", 10));
                assertTrue(model.getRows().getValue().isEmpty());
            } finally { model.onCleared(); }
        });
    }
}
