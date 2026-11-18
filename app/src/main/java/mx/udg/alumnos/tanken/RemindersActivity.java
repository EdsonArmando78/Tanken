package mx.udg.alumnos.tanken;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public final class RemindersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        android.content.Intent intent = MainActivity.destination(this, "reminders");
        if (getIntent().getData() != null)
            intent.putExtra("noticeId", getIntent().getData().getLastPathSegment());
        startActivity(intent);
        finish();
    }
}
