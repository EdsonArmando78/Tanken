package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class WellnessActivity extends AppCompatActivity {
    private EditText interval;
    private CheckBox inactivity;
    private Button start, cancel;
    private TextView status, result, probe;
    private ProgressBar progress;
    private int taps;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wellness);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.wellnessRoot), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        interval = findViewById(R.id.wellnessInterval);
        inactivity = findViewById(R.id.wellnessInactivity);
        start = findViewById(R.id.wellnessStart);
        cancel = findViewById(R.id.wellnessCancel);
        status = findViewById(R.id.wellnessStatus);
        result = findViewById(R.id.wellnessResult);
        progress = findViewById(R.id.wellnessProgress);
        probe = findViewById(R.id.wellnessProbe);
        taps = savedInstanceState == null ? 0 : savedInstanceState.getInt("taps");
        updateProbe();
        probe.setOnClickListener(v -> { taps++; updateProbe(); });
        WellnessViewModel model = new ViewModelProvider(this).get(WellnessViewModel.class);
        start.setOnClickListener(v -> {
            try {
                int minutes = Integer.parseInt(interval.getText().toString().trim());
                if (minutes < 5 || minutes > 240) throw new IllegalArgumentException();
                interval.setError(null);
                model.start(minutes, inactivity.isChecked());
            } catch (IllegalArgumentException invalid) {
                interval.setError(getString(R.string.wellness_invalid_interval));
            }
        });
        cancel.setOnClickListener(v -> model.cancel());
        findViewById(R.id.wellnessBack).setOnClickListener(v -> finish());
        model.getState().observe(this, this::render);
        AppStore.get(this).error().observe(this, error -> {
            if (!error.isEmpty()) com.google.android.material.snackbar.Snackbar.make(status, error,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
        });
    }

    private void updateProbe() { probe.setText(getString(R.string.wellness_probe, taps)); }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("taps", taps);
        super.onSaveInstanceState(outState);
    }

    private void render(WellnessViewModel.State state) {
        boolean running = state.status == WellnessViewModel.Status.RUNNING;
        start.setEnabled(!running);
        cancel.setEnabled(running);
        interval.setEnabled(!running);
        inactivity.setEnabled(!running);
        progress.setVisibility(running ? View.VISIBLE : View.GONE);
        progress.setProgress(state.progress);
        switch (state.status) {
            case RUNNING: status.setText(getString(R.string.wellness_running, state.progress)); break;
            case COMPLETED: status.setText(getString(R.string.wellness_completed, state.elapsedMillis)); break;
            case CANCELLED: status.setText(R.string.wellness_cancelled); break;
            case ERROR: status.setText(R.string.wellness_error); break;
            default: status.setText(R.string.wellness_ready);
        }
        if (state.comparison == null) { result.setText(""); return; }
        WellnessSimulation.Comparison comparison = state.comparison;
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z", Locale.getDefault());
        StringBuilder report = new StringBuilder(getString(R.string.wellness_report_header,
                comparison.start.format(date), comparison.end.format(date), comparison.intervalMinutes,
                comparison.lessonCount, getString(comparison.simulatedInactivity
                        ? R.string.wellness_inactive : R.string.wellness_active)));
        appendMode(report, getString(R.string.wellness_fixed), comparison.fixed);
        appendMode(report, getString(R.string.wellness_contextual), comparison.contextual);
        report.append(getString(R.string.wellness_execution, state.workerName));
        result.setText(report);
    }

    private void appendMode(StringBuilder report, String title, List<WellnessSimulation.Event> events) {
        int emitted = 0, duringClass = 0, rescheduled = 0;
        long delay = 0;
        for (WellnessSimulation.Event event : events) {
            if (!event.rescheduled.isEmpty()) rescheduled++;
            if (event.delivery != null) {
                emitted++;
                if (event.duringClass) duringClass++;
                delay += event.delaySeconds();
            }
        }
        String proportion = emitted == 0 ? getString(R.string.wellness_not_calculable)
                : String.format(Locale.getDefault(), "%.1f%%", 100.0 * duringClass / emitted);
        String average = emitted == 0 ? getString(R.string.wellness_not_calculable)
                : getString(R.string.wellness_seconds, (double) delay / emitted);
        report.append(getString(R.string.wellness_mode_summary, title, events.size(), emitted,
                duringClass, proportion, rescheduled, events.size() - emitted, average));
        DateTimeFormatter time = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss", Locale.getDefault());
        for (WellnessSimulation.Event event : events) {
            report.append(getString(R.string.wellness_event, event.id,
                    getString(event.kind == WellnessSimulation.Kind.HYDRATION
                            ? R.string.wellness_hydration : R.string.wellness_break),
                    event.originalDue.format(time), event.delivery == null
                            ? getString(R.string.wellness_pending) : event.delivery.format(time)));
            for (java.time.ZonedDateTime change : event.rescheduled) {
                report.append(getString(R.string.wellness_rescheduled, change.format(time)));
            }
        }
    }
}
