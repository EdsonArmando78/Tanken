package mx.udg.alumnos.tanken;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns the worker across rotation; neither handler holds an Activity or a View. */
public class WellnessViewModel extends AndroidViewModel {
    private static final int RUN = 1, PROGRESS = 2, RESULT = 3, ERROR = 4;

    public enum Status {
        READY,
        RUNNING,
        COMPLETED,
        CANCELLED,
        ERROR
    }

    public static final class State {
        public final Status status;
        public final int progress;
        public final WellnessSimulation.Comparison comparison;
        public final long elapsedMillis;
        public final String workerName;

        private State(
                Status status,
                int progress,
                WellnessSimulation.Comparison comparison,
                long elapsed,
                String workerName) {
            this.status = status;
            this.progress = progress;
            this.comparison = comparison;
            elapsedMillis = elapsed;
            this.workerName = workerName;
        }
    }

    private static final class Request {
        final String id = java.util.UUID.randomUUID().toString();
        final List<AcademicSchedule.Lesson> lessons;
        final int interval;
        final boolean inactive;
        final ZonedDateTime start;
        final long executedAt = System.currentTimeMillis();
        final long started = SystemClock.elapsedRealtime();
        final AtomicBoolean cancelled = new AtomicBoolean();

        Request(
                int interval,
                boolean inactive,
                List<AcademicSchedule.Lesson> lessons,
                ZonedDateTime start) {
            this.start = start;
            this.interval = interval;
            this.inactive = inactive;
            this.lessons = lessons;
        }
    }

    private static final class Reply {
        final Request request;
        final int progress;
        final WellnessSimulation.Comparison comparison;
        final long elapsed;
        final String workerName;

        Reply(Request request, int progress, WellnessSimulation.Comparison comparison) {
            this.request = request;
            this.progress = progress;
            this.comparison = comparison;
            elapsed = SystemClock.elapsedRealtime() - request.started;
            workerName = Thread.currentThread().getName();
        }
    }

    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(Status.READY, 0, null, 0, ""));
    private final Object deliveryLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper(), this::receive);
    private final HandlerThread handlerThread = new HandlerThread("HiloFondo");
    private final Handler backgroundHandler;
    private volatile boolean closed;
    // Accessed only on the main thread. Workers receive immutable requests via Message.obj.
    private Request active;

    interface WorkGate {
        void await(AtomicBoolean cancelled) throws InterruptedException;
    }

    private final WorkGate gate;

    public WellnessViewModel(Application application) {
        this(application, cancelled -> {});
    }

    WellnessViewModel(Application application, WorkGate gate) {
        super(application);
        this.gate = gate;
        handlerThread.start();
        backgroundHandler =
                new Handler(
                        handlerThread.getLooper(),
                        message -> {
                            if (message.what == RUN) process((Request) message.obj);
                            return true;
                        });
    }

    public LiveData<State> getState() {
        return state;
    }

    @MainThread
    public void start(int interval, boolean inactive) {
        if (closed || active != null) return;
        if (interval < 5 || interval > 240) throw new IllegalArgumentException("Invalid interval");
        AppStore store = AppStore.get(getApplication());
        if (!store.ready()) {
            state.setValue(new State(Status.ERROR, 0, null, 0, ""));
            return;
        }
        active = new Request(interval, inactive, store.snapshot(), ZonedDateTime.now().withNano(0));
        state.setValue(new State(Status.RUNNING, 0, null, 0, ""));
        backgroundHandler.obtainMessage(RUN, active).sendToTarget();
    }

    @MainThread
    public void cancel() {
        if (closed || active == null) return;
        active.cancelled.set(true);
        active = null;
        backgroundHandler.removeMessages(RUN);
        state.setValue(new State(Status.CANCELLED, 0, null, 0, ""));
    }

    private void process(Request request) {
        try {
            if (!pause(request)) return;
            List<AcademicSchedule.Lesson> lessons = request.lessons;
            send(PROGRESS, new Reply(request, 1, null));
            if (!pause(request)) return;
            WellnessSimulation.Comparison result =
                    WellnessSimulation.compare(
                            lessons, request.start, request.interval, request.inactive);
            send(PROGRESS, new Reply(request, 2, null));
            if (!pause(request)) return;
            send(RESULT, new Reply(request, 3, result));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException failure) {
            send(ERROR, new Reply(request, 0, null));
        }
    }

    private boolean pause(Request request) throws InterruptedException {
        gate.await(request.cancelled);
        return !closed && !request.cancelled.get();
    }

    @MainThread
    public void replay(String payload) {
        if (closed || active != null) return;
        try {
            ComparisonCodec.Input input = ComparisonCodec.input(payload);
            active = new Request(input.interval, input.inactive, input.lessons, input.start);
            state.setValue(new State(Status.RUNNING, 0, null, 0, ""));
            backgroundHandler.obtainMessage(RUN, active).sendToTarget();
        } catch (RuntimeException invalid) {
            replayFailed();
        }
    }

    @MainThread
    public void replayFailed() {
        if (!closed) state.setValue(new State(Status.ERROR, 0, null, 0, ""));
    }

    private void send(int what, Reply reply) {
        synchronized (deliveryLock) {
            if (!closed && !reply.request.cancelled.get()) {
                mainHandler.obtainMessage(what, reply).sendToTarget();
            }
        }
    }

    private boolean receive(Message message) {
        Reply reply = (Reply) message.obj;
        // A late result from a cancelled run must never replace a newer run.
        if (closed || reply.request != active || reply.request.cancelled.get()) return true;
        Status status =
                message.what == RESULT
                        ? Status.COMPLETED
                        : message.what == ERROR ? Status.ERROR : Status.RUNNING;
        if (status == Status.COMPLETED)
            AppStore.get(getApplication())
                    .saveComparison(
                            reply.request.id,
                            reply.request.lessons,
                            reply.comparison,
                            reply.request.executedAt);
        if (status != Status.RUNNING) active = null;
        state.setValue(
                new State(
                        status, reply.progress, reply.comparison, reply.elapsed, reply.workerName));
        return true;
    }

    @Override
    protected void onCleared() {
        synchronized (deliveryLock) {
            closed = true;
            if (active != null) active.cancelled.set(true);
            active = null;
            backgroundHandler.removeCallbacksAndMessages(null);
            mainHandler.removeCallbacksAndMessages(null);
            handlerThread.quit();
            handlerThread.interrupt(); // Also releases controlled work when the ViewModel closes.
        }
    }
}
