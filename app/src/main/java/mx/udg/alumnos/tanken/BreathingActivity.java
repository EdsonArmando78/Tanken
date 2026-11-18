package mx.udg.alumnos.tanken;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.ActivityBreathingBinding;

import java.util.Locale;

public final class BreathingActivity extends AppCompatActivity {
    private ActivityBreathingBinding b;
    private BreathingViewModel model;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityBreathingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(BreathingViewModel.class);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.start.setOnClickListener(v -> model.start());
        b.pause.setOnClickListener(v -> model.pause());
        b.stop.setOnClickListener(v -> model.stop());
        model.state()
                .observe(
                        this,
                        s -> {
                            int phase =
                                    s.status == BreathingViewModel.Status.READY
                                            ? R.string.breathing_ready
                                            : s.status == BreathingViewModel.Status.PAUSED
                                                    ? R.string.breathing_paused
                                                    : s.status
                                                                    == BreathingViewModel.Status
                                                                            .COMPLETED
                                                            ? R.string.breathing_complete
                                                            : s.inhale
                                                                    ? R.string.breathing_inhale
                                                                    : R.string.breathing_exhale;
                            b.phase.setText(phase);
                            long seconds = (120_000 - s.elapsed + 999) / 1000;
                            b.remaining.setText(
                                    getString(
                                            R.string.breathing_remaining,
                                            String.format(
                                                    Locale.getDefault(),
                                                    "%02d:%02d",
                                                    seconds / 60,
                                                    seconds % 60)));
                            float scale =
                                    android.animation.ValueAnimator.areAnimatorsEnabled()
                                            ? 0.65f + 0.35f * s.expansion
                                            : 1;
                            b.orb.setScaleX(scale);
                            b.orb.setScaleY(scale);
                            b.start.setText(
                                    s.status == BreathingViewModel.Status.PAUSED
                                            ? R.string.breathing_continue
                                            : R.string.breathing_start);
                            b.start.setEnabled(s.status != BreathingViewModel.Status.RUNNING);
                            b.pause.setEnabled(s.status == BreathingViewModel.Status.RUNNING);
                            b.stop.setEnabled(s.status != BreathingViewModel.Status.READY);
                        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        model.visible();
    }

    @Override
    protected void onStop() {
        model.hidden(isChangingConfigurations());
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        model.snapshot();
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
