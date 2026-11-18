package mx.udg.alumnos.tanken;

import static org.junit.Assert.*;

import android.app.Application;
import android.os.Looper;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class WellnessThreadsTest {
    @Before
    public void awaitInitialScheduleLoad() throws Exception {
        AppStore store =
                AppStore.get(InstrumentationRegistry.getInstrumentation().getTargetContext());
        store.io.submit(() -> {}).get(10, TimeUnit.SECONDS);
        assertTrue("El horario debe estar disponible antes del experimento", store.ready());
    }

    private void main(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private WellnessViewModel model() {
        return new WellnessViewModel(
                (Application)
                        InstrumentationRegistry.getInstrumentation()
                                .getTargetContext()
                                .getApplicationContext());
    }

    @Test
    public void resultsUseWorkerMessagesAndObserverRunsOnMainWithoutBlocking() throws Exception {
        AtomicReference<WellnessViewModel> reference = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Observer<WellnessViewModel.State> observer =
                state -> {
                    assertSame(Looper.getMainLooper(), Looper.myLooper());
                    if (state.status == WellnessViewModel.Status.COMPLETED) {
                        assertEquals("HiloFondo", state.workerName);
                        assertNotNull(state.comparison);
                        assertEquals(60, state.comparison.intervalMinutes);
                        completed.countDown();
                    }
                };
        main(
                () -> {
                    WellnessViewModel model = model();
                    reference.set(model);
                    model.getState().observeForever(observer);
                    model.start(60, true);
                    model.start(5, false); // A double tap must not enqueue a second run.
                    assertEquals(
                            WellnessViewModel.Status.RUNNING, model.getState().getValue().status);
                });
        try {
            CountDownLatch responsive = new CountDownLatch(1);
            new android.os.Handler(Looper.getMainLooper()).post(responsive::countDown);
            assertTrue(
                    "El hilo principal debe responder durante el trabajo",
                    responsive.await(1, TimeUnit.SECONDS));
            assertTrue(completed.await(8, TimeUnit.SECONDS));
        } finally {
            main(
                    () -> {
                        reference.get().getState().removeObserver(observer);
                        reference.get().onCleared();
                    });
        }
    }

    @Test
    public void cancelledRunCannotReplaceNewRun() throws Exception {
        AtomicReference<WellnessViewModel> reference = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean first =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        Observer<WellnessViewModel.State> observer =
                state -> {
                    if (state.status == WellnessViewModel.Status.RUNNING && state.progress == 1) {
                        processing.countDown();
                    }
                    if (state.status == WellnessViewModel.Status.COMPLETED) {
                        assertEquals(15, state.comparison.intervalMinutes);
                        assertFalse(state.comparison.simulatedInactivity);
                        completed.countDown();
                    }
                };
        main(
                () -> {
                    WellnessViewModel model =
                            new WellnessViewModel(
                                    (Application)
                                            InstrumentationRegistry.getInstrumentation()
                                                    .getTargetContext()
                                                    .getApplicationContext(),
                                    cancelled -> {
                                        if (first.getAndSet(false)) {
                                            processing.countDown();
                                            release.await();
                                        }
                                    });
                    reference.set(model);
                    model.getState().observeForever(observer);
                    model.start(60, true);
                });
        try {
            assertTrue(
                    "Debe cancelar una tarea que ya comenzó",
                    processing.await(5, TimeUnit.SECONDS));
            main(
                    () -> {
                        reference.get().cancel();
                        assertEquals(
                                WellnessViewModel.Status.CANCELLED,
                                reference.get().getState().getValue().status);
                        reference.get().start(15, false);
                        release.countDown();
                    });
            assertTrue(completed.await(8, TimeUnit.SECONDS));
        } finally {
            main(
                    () -> {
                        reference.get().getState().removeObserver(observer);
                        reference.get().onCleared();
                    });
        }
    }

    @Test
    public void rotationRetainsTaskAndCloseStopsWorker() throws Exception {
        AtomicReference<WellnessViewModel> reference = new AtomicReference<>();
        java.util.List<Thread> workers = new java.util.ArrayList<>();
        try (ActivityScenario<WellnessActivity> scenario =
                ActivityScenario.launch(WellnessActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        WellnessViewModel model =
                                new ViewModelProvider(activity).get(WellnessViewModel.class);
                        reference.set(model);
                        model.start(30, true);
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            if (thread.getName().equals("HiloFondo")) workers.add(thread);
                        }
                    });
            scenario.recreate();
            scenario.onActivity(
                    activity ->
                            assertSame(
                                    reference.get(),
                                    new ViewModelProvider(activity).get(WellnessViewModel.class)));
        }
        assertFalse(workers.isEmpty());
        for (Thread worker : workers) {
            worker.join(2000); // Test thread only; the Activity never joins a worker.
            assertFalse(worker.isAlive());
        }
        main(
                () -> {
                    WellnessViewModel.State previous = reference.get().getState().getValue();
                    reference.get().start(10, true);
                    assertSame(previous, reference.get().getState().getValue());
                });
    }
}
