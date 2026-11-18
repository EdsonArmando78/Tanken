package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.ActivityWellnessBinding;

public final class WellnessActivity extends AppCompatActivity {
    private ActivityWellnessBinding b;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityWellnessBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        WellnessViewModel model = new ViewModelProvider(this).get(WellnessViewModel.class);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.wellnessInterval.setText(getString(R.string.wellness_default_interval));
        b.wellnessStart.setOnClickListener(
                v -> {
                    try {
                        int minutes =
                                Integer.parseInt(b.wellnessInterval.getText().toString().trim());
                        if (minutes < 5 || minutes > 240) throw new IllegalArgumentException();
                        b.wellnessIntervalLayout.setError(null);
                        model.start(minutes, b.wellnessInactivity.isChecked());
                    } catch (IllegalArgumentException invalid) {
                        b.wellnessIntervalLayout.setError(
                                getString(R.string.wellness_invalid_interval));
                    }
                });
        b.wellnessCancel.setOnClickListener(v -> model.cancel());
        model.getState()
                .observe(
                        this,
                        state -> {
                            boolean running = state.status == WellnessViewModel.Status.RUNNING;
                            b.wellnessStart.setEnabled(!running);
                            b.wellnessCancel.setEnabled(running);
                            b.wellnessInterval.setEnabled(!running);
                            b.wellnessInactivity.setEnabled(!running);
                            b.wellnessProgress.setVisibility(running ? View.VISIBLE : View.GONE);
                            b.wellnessProgress.setProgress(state.progress);
                            switch (state.status) {
                                case RUNNING:
                                    b.wellnessStatus.setText(
                                            getString(R.string.wellness_running, state.progress));
                                    break;
                                case COMPLETED:
                                    b.wellnessStatus.setText(
                                            getString(
                                                    R.string.wellness_completed,
                                                    state.elapsedMillis));
                                    break;
                                case CANCELLED:
                                    b.wellnessStatus.setText(R.string.wellness_cancelled);
                                    break;
                                case ERROR:
                                    b.wellnessStatus.setText(R.string.wellness_error);
                                    break;
                                default:
                                    b.wellnessStatus.setText(R.string.wellness_ready);
                            }
                            b.wellnessResult.setText(
                                    state.comparison == null
                                            ? ""
                                            : ComparisonReport.format(this, state.comparison));
                        });
        AppStore.get(this)
                .error()
                .observe(
                        this,
                        error -> {
                            if (!error.isEmpty())
                                com.google.android.material.snackbar.Snackbar.make(
                                                b.getRoot(),
                                                error,
                                                com.google.android.material.snackbar.Snackbar
                                                        .LENGTH_LONG)
                                        .show();
                        });
        String id = getIntent().getStringExtra("comparisonId");
        if (saved == null && id != null)
            AppStore.get(this)
                    .loadComparison(
                            id,
                            row -> {
                                if (row == null) model.replayFailed();
                                else model.replay(row.inputAndResults);
                            });
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
