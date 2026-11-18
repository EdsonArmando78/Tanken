package mx.udg.alumnos.tanken;

import java.time.LocalTime;

/** Immutable settings for the future scheduler; defaults do not enable any alarms. */
public final class WellnessPreferences {
    public static final int FORMAT_VERSION = 1;
    public final boolean hydrationEnabled, breaksEnabled, classesEnabled;
    public final int hydrationMinutes, breakMinutes, classLeadMinutes, dayMask;
    public final LocalTime silenceStart, silenceEnd;

    public WellnessPreferences(
            boolean hydrationEnabled,
            boolean breaksEnabled,
            boolean classesEnabled,
            int hydrationMinutes,
            int breakMinutes,
            int classLeadMinutes,
            int dayMask,
            LocalTime silenceStart,
            LocalTime silenceEnd) {
        if (hydrationMinutes < 5
                || hydrationMinutes > 240
                || breakMinutes < 5
                || breakMinutes > 240
                || classLeadMinutes < 1
                || classLeadMinutes > 120
                || dayMask < 1
                || dayMask > 127
                || silenceStart == null
                || silenceEnd == null) throw new IllegalArgumentException("Invalid settings");
        this.hydrationEnabled = hydrationEnabled;
        this.breaksEnabled = breaksEnabled;
        this.classesEnabled = classesEnabled;
        this.hydrationMinutes = hydrationMinutes;
        this.breakMinutes = breakMinutes;
        this.classLeadMinutes = classLeadMinutes;
        this.dayMask = dayMask;
        this.silenceStart = silenceStart;
        this.silenceEnd = silenceEnd;
    }

    public static WellnessPreferences defaults() {
        return new WellnessPreferences(
                false, false, false, 60, 60, 10, 31, LocalTime.of(22, 0), LocalTime.of(7, 0));
    }
}
