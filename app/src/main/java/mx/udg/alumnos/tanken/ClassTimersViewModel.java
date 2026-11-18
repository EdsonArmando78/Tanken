package mx.udg.alumnos.tanken;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All mutable state belongs to the main thread; each worker owns its CountDownTimer. */
public class ClassTimersViewModel extends AndroidViewModel {
    static final int MAX_WORKERS = 4;

    public enum Status {
        WAITING,
        RUNNING,
        FINISHED,
        CANCELLED
    }

    public static final class Row {
        public final int id;
        public final String name;
        public final long secondsLeft;
        public final Status status;

        Row(Entry entry) {
            id = entry.id;
            name = entry.name;
            secondsLeft = entry.secondsLeft;
            status = entry.status;
        }
    }

    private static final class Entry {
        final int id;
        final String name;
        final long durationMillis;
        long secondsLeft;
        Status status = Status.WAITING;
        int worker = -1;
        // Read/written only on the assigned worker's Looper.
        CountDownTimer timer;

        Entry(int id, String name, long seconds) {
            this.id = id;
            this.name = name;
            secondsLeft = seconds;
            durationMillis = seconds * 1000;
        }
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread[] threads = new HandlerThread[MAX_WORKERS];
    private final Handler[] workers = new Handler[MAX_WORKERS];
    private final boolean[] busy = new boolean[MAX_WORKERS];
    private final Map<Integer, Entry> entries = new LinkedHashMap<>();
    private final MutableLiveData<List<Row>> rows = new MutableLiveData<>(Collections.emptyList());
    private int nextId = 1;
    private boolean closed;
    private final long generation;

    public ClassTimersViewModel(Application application) {
        super(application);
        generation = AppStore.get(application).generation();
        TimerNotifications.createChannel(application);
    }

    public LiveData<List<Row>> getRows() {
        return rows;
    }

    @MainThread
    public void add(String name, long seconds) {
        if (closed) return;
        if (name == null || name.trim().isEmpty() || seconds < 1 || seconds > 86400) {
            throw new IllegalArgumentException(
                    getApplication().getString(R.string.timer_invalid_input));
        }
        Entry entry = new Entry(nextId++, name.trim(), seconds);
        entries.put(entry.id, entry);
        dispatch();
        publish();
    }

    @MainThread
    public void cancel(int id) {
        Entry entry = entries.get(id);
        if (closed
                || entry == null
                || (entry.status != Status.RUNNING && entry.status != Status.WAITING)) return;
        boolean running = entry.status == Status.RUNNING;
        entry.status = Status.CANCELLED;
        if (running) {
            // FIFO: cancel the old counter before reusing this worker for another entry.
            workers[entry.worker].post(
                    () -> {
                        if (entry.timer != null) entry.timer.cancel();
                    });
            busy[entry.worker] = false;
        }
        dispatch();
        publish();
    }

    @MainThread
    public void clearHistory() {
        entries.values().removeIf(e -> e.status == Status.FINISHED || e.status == Status.CANCELLED);
        publish();
    }

    private void dispatch() {
        for (Entry entry : entries.values()) {
            if (entry.status != Status.WAITING) continue;
            int slot = 0;
            while (slot < MAX_WORKERS && busy[slot]) slot++;
            if (slot == MAX_WORKERS) return;
            busy[slot] = true;
            entry.worker = slot;
            entry.status = Status.RUNNING;
            if (workers[slot] == null) {
                threads[slot] = new HandlerThread("Tanken-timer-" + (slot + 1));
                threads[slot].start();
                workers[slot] = new Handler(threads[slot].getLooper());
            }
            final long deadline = SystemClock.elapsedRealtime() + entry.durationMillis;
            workers[slot].post(
                    () -> {
                        entry.timer =
                                new CountDownTimer(
                                        Math.max(1, deadline - SystemClock.elapsedRealtime()),
                                        1000) {
                                    @Override
                                    public void onTick(long remaining) {
                                        main.post(
                                                () -> {
                                                    if (closed
                                                            || generation
                                                                    != AppStore.get(
                                                                                    getApplication())
                                                                            .generation()
                                                            || entry.status != Status.RUNNING)
                                                        return;
                                                    entry.secondsLeft = (remaining + 999) / 1000;
                                                    publish();
                                                });
                                    }

                                    @Override
                                    public void onFinish() {
                                        main.post(() -> finish(entry));
                                    }
                                }.start();
                    });
        }
    }

    private void finish(Entry entry) {
        if (closed
                || generation != AppStore.get(getApplication()).generation()
                || entry.status != Status.RUNNING) return;
        entry.status = Status.FINISHED;
        entry.secondsLeft = 0;
        busy[entry.worker] = false;
        TimerNotifications.show(getApplication(), entry.id, entry.name);
        dispatch();
        publish();
    }

    private void publish() {
        List<Row> snapshot = new ArrayList<>();
        for (Entry entry : entries.values()) snapshot.add(new Row(entry));
        rows.setValue(Collections.unmodifiableList(snapshot));
    }

    @Override
    protected void onCleared() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        for (Entry entry : entries.values()) {
            if (entry.worker >= 0) {
                workers[entry.worker].post(
                        () -> {
                            if (entry.timer != null) entry.timer.cancel();
                        });
            }
        }
        for (HandlerThread thread : threads) {
            if (thread != null) thread.quitSafely();
        }
    }
}
