package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import mx.udg.alumnos.tanken.databinding.ActivityHelpBinding;

public final class HelpActivity extends AppCompatActivity {
    private ActivityHelpBinding b;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.replayTutorial.setOnClickListener(
                v ->
                        startActivity(
                                new Intent(this, OnboardingActivity.class)
                                        .putExtra(OnboardingActivity.REPLAY, true)));
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
