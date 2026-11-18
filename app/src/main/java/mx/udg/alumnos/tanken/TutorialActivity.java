package mx.udg.alumnos.tanken;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TutorialActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutorial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        VideoView video = findViewById(R.id.videoView);
        Uri uri = Uri.parse(String.format("android.resource://%s/%s", getPackageName(), R.raw.video));
        video.setVideoURI(uri);
        video.setOnClickListener(v -> {
            if (video.isPlaying())
                video.pause();
            else
                video.start();
        });
        video.start();
    }
}
