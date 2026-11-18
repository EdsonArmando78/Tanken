package mx.udg.alumnos.tanken;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

final class TimerDuration {
    private TimerDuration() {}

    static long seconds(String hours, String minutes, String seconds) {
        int h = number(hours), m = number(minutes), s = number(seconds);
        long total = h * 3600L + m * 60L + s;
        if (h < 0 || h > 24 || m < 0 || m > 59 || s < 0 || s > 59 || total < 1 || total > 86400)
            throw new IllegalArgumentException("Duration must be between one second and 24 hours");
        return total;
    }

    private static int number(String value) {
        return value.trim().isEmpty() ? 0 : Integer.parseInt(value.trim());
    }

    static String label(Context context, long seconds) {
        List<String> parts = new ArrayList<>();
        int[] values = {(int) (seconds / 3600), (int) (seconds / 60 % 60), (int) (seconds % 60)};
        int[] units = {R.plurals.timer_hours, R.plurals.timer_minutes, R.plurals.timer_seconds};
        for (int i = 0; i < values.length; i++)
            if (values[i] > 0)
                parts.add(context.getResources().getQuantityString(units[i], values[i], values[i]));
        return String.join(", ", parts);
    }
}
