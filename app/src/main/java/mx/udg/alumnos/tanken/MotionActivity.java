package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.ActivityMotionBinding;

public class MotionActivity extends AppCompatActivity {
    private ActivityMotionBinding b;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityMotionBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        OverviewViewModel model = new ViewModelProvider(this).get(OverviewViewModel.class);
        model.motion()
                .observe(
                        this,
                        s -> {
                            b.status.setText(ProductUi.motion(this, s));
                            b.stamp.setText(ProductUi.stamp(this, s));
                            b.xValue.setText(
                                    s.at == 0
                                            ? getString(R.string.reading_axis_missing, "X")
                                            : getString(R.string.reading_axis, "X", s.x));
                            b.yValue.setText(
                                    s.at == 0
                                            ? getString(R.string.reading_axis_missing, "Y")
                                            : getString(R.string.reading_axis, "Y", s.y));
                            b.zValue.setText(
                                    s.at == 0
                                            ? getString(R.string.reading_axis_missing, "Z")
                                            : getString(R.string.reading_axis, "Z", s.z));
                            b.xBar.setProgress(Math.min(2000, Math.round(Math.abs(s.x) * 100)));
                            b.yBar.setProgress(Math.min(2000, Math.round(Math.abs(s.y) * 100)));
                            b.zBar.setProgress(Math.min(2000, Math.round(Math.abs(s.z) * 100)));
                            b.summary.setText(
                                    getString(
                                            R.string.reading_session,
                                            s.samples,
                                            s.observedMillis / 1000,
                                            s.movement,
                                            s.peak));
                            b.pause.setText(
                                    s.enabled ? R.string.reading_pause : R.string.reading_resume);
                            b.pause.setIconResource(
                                    s.enabled
                                            ? R.drawable.ic_ms_pause
                                            : R.drawable.ic_ms_play_arrow);
                            b.pause.setOnClickListener(v -> model.setReading(!s.enabled));
                        });
        model.error()
                .observe(
                        this,
                        e -> {
                            b.storageError.setText(e);
                            b.storageError.setVisibility(e.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        b.reset.setOnClickListener(v -> model.reset());
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
