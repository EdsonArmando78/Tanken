package mx.udg.alumnos.tanken;

import android.content.Context;
import android.view.View;

import androidx.core.view.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ProductUi {
    private ProductUi() {}

    static void insets(View root) {
        int left = root.getPaddingLeft(),
                top = root.getPaddingTop(),
                right = root.getPaddingRight(),
                bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (v, i) -> {
                    androidx.core.graphics.Insets b =
                            i.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.ime());
                    v.setPadding(left + b.left, top + b.top, right + b.right, bottom + b.bottom);
                    return i;
                });
    }

    static String motion(Context c, MotionSource.State s) {
        int label;
        switch (s.availability) {
            case MISSING:
                label = R.string.motion_missing;
                break;
            case ERROR:
                label = R.string.motion_failed;
                break;
            case PAUSED:
                label = R.string.reading_paused;
                break;
            case BACKGROUND:
                label = R.string.reading_background;
                break;
            case STALE:
                label = R.string.reading_stale;
                break;
            case STILL:
                label = R.string.motion_still;
                break;
            case MOVING:
                label = R.string.motion_moving;
                break;
            case STRONG:
                label = R.string.motion_strong;
                break;
            default:
                label = R.string.motion_waiting;
        }
        return c.getString(label);
    }

    static String stamp(Context c, MotionSource.State s) {
        return s.at == 0
                ? c.getString(R.string.reading_no_sample)
                : c.getString(
                        R.string.reading_stamp,
                        Instant.ofEpochMilli(s.at)
                                .atZone(ZoneId.systemDefault())
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "dd/MM HH:mm:ss", Locale.getDefault())),
                        Math.max(0, s.ageMillis) / 1000);
    }
}
