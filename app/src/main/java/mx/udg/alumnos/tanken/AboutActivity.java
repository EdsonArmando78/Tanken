package mx.udg.alumnos.tanken;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import mx.udg.alumnos.tanken.databinding.ActivityAboutBinding;

public final class AboutActivity extends AppCompatActivity {
    private ActivityAboutBinding b;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        try {
            b.version.setText(
                    getString(
                            R.string.about_version,
                            getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (android.content.pm.PackageManager.NameNotFoundException impossible) {
            b.version.setText(R.string.app_name);
        }
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
