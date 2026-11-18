package mx.udg.alumnos.tanken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimersActivity extends AppCompatActivity {
    private ClassTimersViewModel model;
    private List<AcademicSchedule.Lesson> lessons;
    private LinearLayout rows;
    private TextView summary, notifications;
    private EditText name, seconds;
    private final Map<Integer, TimerRow> views = new HashMap<>();
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> updateNotificationStatus());

    private static final class TimerRow {
        final LinearLayout layout;
        final TextView text;
        final Button cancel;
        TimerRow(LinearLayout layout, TextView text, Button cancel) {
            this.layout = layout;
            this.text = text;
            this.cancel = cancel;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        model = new ViewModelProvider(this).get(ClassTimersViewModel.class);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);
        TextView title = new TextView(this);
        title.setText(R.string.timers_title);
        title.setTextSize(24);
        content.addView(title);
        TextView help = new TextView(this);
        help.setText(R.string.timers_help);
        content.addView(help);
        name = new EditText(this);
        name.setId(R.id.timerName);
        name.setHint(getString(R.string.timer_name_hint));
        name.setSingleLine(true);
        content.addView(name);
        TextView durationLabel = new TextView(this);
        durationLabel.setText(getString(R.string.timer_duration_label));
        content.addView(durationLabel);
        seconds = new EditText(this);
        seconds.setId(R.id.timerSeconds);
        seconds.setHint(getString(R.string.timer_duration_label));
        seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        seconds.setSingleLine(true);
        seconds.setText(getString(R.string.timer_default_duration));
        content.addView(seconds);
        button(content, getString(R.string.timer_next_class), this::useNextClass);
        button(content, getString(R.string.timer_start), this::addTimer);
        notifications = new TextView(this);
        content.addView(notifications);
        button(content, getString(R.string.timer_enable_notifications), () -> {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && !getPreferences(MODE_PRIVATE).getBoolean("notificationAsked", false)) {
                getPreferences(MODE_PRIVATE).edit().putBoolean("notificationAsked", true).apply();
                permission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
        summary = new TextView(this);
        content.addView(summary);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        content.addView(rows);
        button(content, getString(R.string.timer_clear_history), model::clearHistory);
        button(content, getString(R.string.timer_back), this::finish);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        model.getRows().observe(this, this::render);
        new ScheduleRepository(this).observe().observe(this, value -> lessons = value);
    }

    @Override protected void onResume() {
        super.onResume();
        updateNotificationStatus();
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button);
    }

    private void updateNotificationStatus() {
        android.app.NotificationChannel channel = getSystemService(android.app.NotificationManager.class)
                .getNotificationChannel("class_timers");
        boolean enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
                && channel != null && channel.getImportance() != android.app.NotificationManager.IMPORTANCE_NONE;
        notifications.setText(enabled ? getString(R.string.timer_notifications_enabled)
                : getString(R.string.timer_notifications_disabled));
    }

    private void addTimer() {
        try {
            model.add(name.getText().toString(), Long.parseLong(seconds.getText().toString().trim()));
            name.setError(null);
            seconds.setError(null);
        } catch (NumberFormatException e) {
            seconds.setError(getString(R.string.timer_invalid_duration));
        } catch (IllegalArgumentException e) {
            if (name.getText().toString().trim().isEmpty()) name.setError(getString(R.string.timer_invalid_name));
            else seconds.setError(e.getMessage());
        }
    }

    private void useNextClass() {
        List<ClassTimersViewModel.Row> timers = model.getRows().getValue();
        int active = 0;
        if (timers != null) {
            for (ClassTimersViewModel.Row timer : timers) {
                if (timer.status == ClassTimersViewModel.Status.RUNNING) active++;
            }
        }
        if (active == ClassTimersViewModel.MAX_WORKERS) {
            Toast.makeText(this, getString(R.string.timer_no_free_worker), Toast.LENGTH_LONG).show();
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        if (lessons == null) { Toast.makeText(this, R.string.schedule_loading, Toast.LENGTH_SHORT).show(); return; }
        AcademicSchedule.Occurrence next = AcademicSchedule.at(lessons, now).next;
        if (next == null) {
            Toast.makeText(this, getString(R.string.timer_no_classes), Toast.LENGTH_LONG).show();
            return;
        }
        long duration = (Duration.between(now, next.start).toMillis() + 999) / 1000;
        if (duration > 86400) {
            Toast.makeText(this, getString(R.string.timer_class_too_far), Toast.LENGTH_LONG).show();
            return;
        }
        model.add(getString(R.string.timer_class_name, next.lesson.name), Math.max(1, duration));
    }

    private void render(List<ClassTimersViewModel.Row> timers) {
        int active = 0, waiting = 0;
        java.util.Set<Integer> present = new java.util.HashSet<>();
        for (ClassTimersViewModel.Row timer : timers) present.add(timer.id);
        views.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) return false;
            rows.removeView(entry.getValue().layout);
            return true;
        });
        for (ClassTimersViewModel.Row timer : timers) {
            if (timer.status == ClassTimersViewModel.Status.RUNNING) active++;
            if (timer.status == ClassTimersViewModel.Status.WAITING) waiting++;
            TimerRow row = views.get(timer.id);
            if (row == null) {
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                TextView text = new TextView(this);
                text.setTextSize(18);
                layout.addView(text);
                Button cancel = new Button(this);
                cancel.setText(getString(R.string.timer_cancel, timer.id));
                cancel.setOnClickListener(v -> model.cancel(timer.id));
                layout.addView(cancel);
                row = new TimerRow(layout, text, cancel);
                views.put(timer.id, row);
                rows.addView(layout);
            }
            String status;
            switch (timer.status) {
                case FINISHED: status = getString(R.string.timer_finished); break;
                case CANCELLED: status = getString(R.string.timer_cancelled); break;
                case WAITING: status = getString(R.string.timer_waiting, time(timer.secondsLeft)); break;
                default: status = getString(R.string.timer_running, time(timer.secondsLeft));
            }
            row.text.setText(getString(R.string.timer_row, timer.id, timer.name, status));
            row.cancel.setEnabled(timer.status == ClassTimersViewModel.Status.RUNNING
                    || timer.status == ClassTimersViewModel.Status.WAITING);
        }
        summary.setText(timers.isEmpty() ? getString(R.string.timer_empty)
                : getString(R.string.timer_summary, active, waiting));
    }

    private String time(long seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
