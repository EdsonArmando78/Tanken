package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import mx.udg.alumnos.tanken.databinding.ActivityTimersBinding;

import java.time.*;
import java.util.*;

public final class TimersActivity extends AppCompatActivity {
    private ActivityTimersBinding b;
    private ClassTimersViewModel model;
    private List<AcademicSchedule.Lesson> lessons;
    private final NotificationAccess permission =
            new NotificationAccess(this, () -> this, this::updateNotificationStatus);

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityTimersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(ClassTimersViewModel.class);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.nextClass.setOnClickListener(v -> useNextClass());
        b.start.setOnClickListener(v -> addTimer());
        b.permission.setOnClickListener(v -> permission.requestOrSettings());
        b.clear.setOnClickListener(v -> model.clearHistory());
        TimerAdapter adapter = new TimerAdapter(model::cancel);
        b.timers.setLayoutManager(new LinearLayoutManager(this));
        b.timers.setAdapter(adapter);
        model.getRows()
                .observe(
                        this,
                        rows -> {
                            adapter.submitList(rows);
                            int active = 0, waiting = 0;
                            for (ClassTimersViewModel.Row row : rows) {
                                if (row.status == ClassTimersViewModel.Status.RUNNING) active++;
                                if (row.status == ClassTimersViewModel.Status.WAITING) waiting++;
                            }
                            b.summary.setText(
                                    rows.isEmpty()
                                            ? getString(R.string.timer_empty)
                                            : getString(R.string.timer_summary, active, waiting));
                        });
        new ScheduleRepository(this).observe().observe(this, value -> lessons = value);
    }

    private void updateNotificationStatus() {
        if (b != null)
            b.notifications.setText(
                    NotificationAccess.enabled(this, "class_timers")
                            ? R.string.timer_notifications_enabled
                            : R.string.timer_notifications_disabled);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationStatus();
    }

    private void addTimer() {
        try {
            model.add(
                    b.timerName.getText().toString(),
                    Long.parseLong(b.timerSeconds.getText().toString().trim()));
            b.timerNameLayout.setError(null);
            b.timerSecondsLayout.setError(null);
        } catch (NumberFormatException e) {
            b.timerSecondsLayout.setError(getString(R.string.timer_invalid_duration));
        } catch (IllegalArgumentException e) {
            if (b.timerName.getText().toString().trim().isEmpty())
                b.timerNameLayout.setError(getString(R.string.timer_invalid_name));
            else b.timerSecondsLayout.setError(e.getMessage());
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
            Toast.makeText(this, getString(R.string.timer_no_free_worker), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        if (lessons == null) {
            Toast.makeText(this, R.string.schedule_loading, Toast.LENGTH_SHORT).show();
            return;
        }
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

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
