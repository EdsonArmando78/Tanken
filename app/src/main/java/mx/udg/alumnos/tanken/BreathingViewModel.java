package mx.udg.alumnos.tanken;

import android.os.*;

import androidx.lifecycle.*;

public final class BreathingViewModel extends ViewModel {
    public enum Status {
        READY,
        RUNNING,
        PAUSED,
        COMPLETED
    }

    public static final class State {
        public final Status status;
        public final long elapsed;
        public final boolean inhale;
        public final float expansion;

        State(Status status, long elapsed) {
            this.status = status;
            this.elapsed = elapsed;
            long phase = elapsed % 8000;
            inhale = phase < 4000;
            expansion = phase < 4000 ? phase / 4000f : 1 - (phase - 4000) / 4000f;
        }
    }

    private final SavedStateHandle saved;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private Status status;
    private long accumulated, started;
    private final Runnable tick =
            new Runnable() {
                @Override
                public void run() {
                    update();
                    if (status == Status.RUNNING) handler.postDelayed(this, 50);
                }
            };

    public BreathingViewModel(SavedStateHandle saved) {
        this.saved = saved;
        Long previous = saved.get("elapsed");
        accumulated = previous == null ? 0 : Math.min(120_000, previous);
        String previousStatus = saved.get("status");
        status = previousStatus == null ? Status.READY : Status.valueOf(previousStatus);
        if (status == Status.RUNNING) status = Status.PAUSED;
        update();
    }

    public LiveData<State> state() {
        return state;
    }

    private long elapsed() {
        return Math.min(
                120_000,
                accumulated
                        + (status == Status.RUNNING ? SystemClock.elapsedRealtime() - started : 0));
    }

    private void update() {
        long elapsed = elapsed();
        if (elapsed == 120_000) {
            accumulated = elapsed;
            status = Status.COMPLETED;
            snapshot();
        }
        state.setValue(new State(status, elapsed));
    }

    public void start() {
        if (status == Status.RUNNING) return;
        if (status != Status.PAUSED) accumulated = 0;
        status = Status.RUNNING;
        started = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        tick.run();
        snapshot();
    }

    public void pause() {
        if (status == Status.RUNNING) {
            accumulated = elapsed();
            status = Status.PAUSED;
        }
        handler.removeCallbacks(tick);
        snapshot();
        update();
    }

    public void stop() {
        handler.removeCallbacks(tick);
        accumulated = 0;
        status = Status.READY;
        snapshot();
        update();
    }

    public void snapshot() {
        saved.set("elapsed", elapsed());
        saved.set("status", status.name());
    }

    public void hidden(boolean changingConfiguration) {
        handler.removeCallbacks(tick);
        if (!changingConfiguration) pause();
        else snapshot();
    }

    public void visible() {
        handler.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
    }
}
