package mx.udg.alumnos.tanken;

/** Sensor-time filter and observed-time accounting, independent of Android. */
public final class MotionAccumulator {
    public static final long STALE_NANOS = 5_000_000_000L;
    private final double[] gravity = new double[3];
    private long previous;
    public long samples, observedNanos;
    public double movement, peak;

    public long accept(float x, float y, float z, long timestamp) {
        if (!Float.isFinite(x)
                || !Float.isFinite(y)
                || !Float.isFinite(z)
                || timestamp <= 0
                || (previous != 0 && timestamp <= previous)) return -1;
        long delta = previous == 0 ? 0 : timestamp - previous;
        if (delta >= STALE_NANOS) delta = 0;
        double alpha = delta == 0 ? 0 : Math.exp(-delta / 1_000_000_000.0 / 0.5);
        previous = timestamp;
        float[] values = {x, y, z};
        double squared = 0;
        for (int i = 0; i < 3; i++) {
            gravity[i] = alpha * gravity[i] + (1 - alpha) * values[i];
            double linear = values[i] - gravity[i];
            squared += linear * linear;
        }
        movement = Math.sqrt(squared);
        peak = Math.max(peak, movement);
        samples++;
        observedNanos += delta;
        return delta;
    }

    public void disconnect() {
        previous = 0;
    }

    public void reset() {
        previous = 0;
        samples = 0;
        observedNanos = 0;
        movement = 0;
        peak = 0;
    }
}
