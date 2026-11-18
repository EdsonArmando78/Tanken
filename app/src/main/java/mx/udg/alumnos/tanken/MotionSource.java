package mx.udg.alumnos.tanken;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.*;
import android.os.*;

import androidx.lifecycle.*;

import java.util.UUID;

/** Application-scoped foreground acquisition. No Activity, View, or observer owns the listener. */
public final class MotionSource implements DefaultLifecycleObserver, SensorEventListener {
    public enum Availability {
        WAITING,
        MISSING,
        ERROR,
        PAUSED,
        BACKGROUND,
        STALE,
        STILL,
        MOVING,
        STRONG
    }

    public static final class State {
        public final Availability availability;
        public final float x, y, z;
        public final long at, ageMillis, samples, observedMillis;
        public final long sampleElapsedMillis;
        public final double movement, peak;
        public final boolean enabled;
        public final String sessionId;

        State(
                Availability availability,
                float x,
                float y,
                float z,
                long at,
                long ageMillis,
                MotionAccumulator accumulator,
                boolean enabled,
                String sessionId,
                long sampleElapsedMillis) {
            this.sampleElapsedMillis = sampleElapsedMillis;
            this.availability = availability;
            this.x = x;
            this.y = y;
            this.z = z;
            this.at = at;
            this.ageMillis = ageMillis;
            samples = accumulator.samples;
            observedMillis = accumulator.observedNanos / 1_000_000;
            movement = accumulator.movement;
            peak = accumulator.peak;
            this.enabled = enabled;
            this.sessionId = sessionId;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private volatile State latest;
    private volatile long lastMovementElapsed;

    public boolean recentlyMoving() {
        State value = latest;
        long now = SystemClock.elapsedRealtime();
        return value != null
                && (value.availability == Availability.STILL
                        || value.availability == Availability.MOVING
                        || value.availability == Availability.STRONG)
                && lastMovementElapsed > 0
                && now - lastMovementElapsed < 5000
                && now - value.sampleElapsedMillis < 5000;
    }

    private final SensorManager manager;
    private final Sensor sensor;
    private final Handler worker;
    private final SharedPreferences preferences;
    private final AppStore store;
    private final MotionAccumulator accumulator = new MotionAccumulator();
    private String sessionId = UUID.randomUUID().toString();
    private boolean visible, enabled, listening;
    private Availability availability = Availability.WAITING;
    private float x, y, z;
    private long at, received, lastPublished;
    private long windowStarted, windowSamples, windowObserved;
    private double windowPeak;

    public MotionSource(Context context) {
        store = AppStore.get(context);
        preferences = context.getSharedPreferences("wellness_preferences", Context.MODE_PRIVATE);
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        HandlerThread thread = new HandlerThread("TankenSensors");
        thread.start();
        worker = new Handler(thread.getLooper());
        worker.post(
                () -> {
                    try {
                        enabled = preferences.getBoolean("motionEnabled", true);
                    } catch (RuntimeException failure) {
                        enabled = false;
                    }
                    publish();
                });
    }

    public LiveData<State> state() {
        return state;
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        worker.post(
                () -> {
                    visible = true;
                    reconcile();
                });
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        worker.post(
                () -> {
                    visible = false;
                    reconcile();
                });
    }

    public void setEnabled(boolean value) {
        worker.post(
                () -> {
                    if (!preferences.edit().putBoolean("motionEnabled", value).commit()) {
                        availability = Availability.ERROR;
                        publish();
                        return;
                    }
                    enabled = value;
                    reconcile();
                });
    }

    public void reset() {
        worker.post(
                () -> {
                    flush();
                    accumulator.reset();
                    at = 0;
                    received = 0;
                    sessionId = UUID.randomUUID().toString();
                    lastMovementElapsed = 0;
                    availability = listening ? Availability.WAITING : availability;
                    publish();
                });
    }

    private void reconcile() {
        boolean shouldListen = enabled && visible && sensor != null;
        if (!shouldListen) {
            if (listening) {
                manager.unregisterListener(this);
                listening = false;
            }
            worker.removeCallbacks(checkFreshness);
            flush();
            accumulator.disconnect();
            availability =
                    !enabled
                            ? Availability.PAUSED
                            : sensor == null ? Availability.MISSING : Availability.BACKGROUND;
        } else if (!listening) {
            accumulator.disconnect();
            listening =
                    manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, worker);
            availability = listening ? Availability.WAITING : Availability.ERROR;
            if (listening) worker.postDelayed(checkFreshness, 1000);
        }
        publish();
    }

    private final Runnable checkFreshness =
            new Runnable() {
                @Override
                public void run() {
                    if (!listening) return;
                    if (received == 0 || SystemClock.elapsedRealtime() - received >= 5000) {
                        flush();
                        accumulator.disconnect();
                        availability = Availability.STALE;
                    }
                    publish();
                    worker.postDelayed(this, 1000);
                }
            };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!listening || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        long delta =
                accumulator.accept(
                        event.values[0], event.values[1], event.values[2], event.timestamp);
        if (delta < 0) return;
        long now =
                System.currentTimeMillis()
                        - Math.max(
                                0,
                                (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000);
        if (received != 0 && SystemClock.elapsedRealtime() - received >= 5000) flush();
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        at = now;
        received = SystemClock.elapsedRealtime();
        if (accumulator.movement >= 0.8) lastMovementElapsed = received;
        if (windowSamples == 0) windowStarted = now;
        windowSamples++;
        windowObserved += delta;
        windowPeak = Math.max(windowPeak, accumulator.movement);
        availability =
                delta == 0
                        ? Availability.WAITING
                        : accumulator.movement >= 4
                                ? Availability.STRONG
                                : accumulator.movement >= 0.8
                                        ? Availability.MOVING
                                        : Availability.STILL;
        if (windowObserved >= 60_000_000_000L) flush();
        if (received - lastPublished >= 200) {
            lastPublished = received;
            publish();
        }
    }

    private void flush() {
        if (windowSamples == 0) return;
        TankenDatabase.MotionRow row = new TankenDatabase.MotionRow();
        row.id = UUID.randomUUID().toString();
        row.sessionId = sessionId;
        row.startedAt = windowStarted;
        row.endedAt = at;
        row.samples = windowSamples;
        row.observedMillis = windowObserved / 1_000_000;
        row.peak = windowPeak;
        store.saveMotion(row);
        windowSamples = 0;
        windowObserved = 0;
        windowPeak = 0;
    }

    private void publish() {
        State value =
                new State(
                        availability,
                        x,
                        y,
                        z,
                        at,
                        received == 0 ? -1 : SystemClock.elapsedRealtime() - received,
                        accumulator,
                        enabled,
                        sessionId,
                        received);
        latest = value;
        state.postValue(value);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
