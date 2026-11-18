package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.*;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import mx.udg.alumnos.tanken.databinding.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    private BottomSheetDialog greeting;
    private List<AcademicSchedule.Lesson> lessons = Collections.emptyList();
    private boolean loaded;
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick =
            new Runnable() {
                @Override
                public void run() {
                    renderClock();
                    clock.postDelayed(this, 1000 - Math.floorMod(System.currentTimeMillis(), 1000));
                }
            };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        OverviewViewModel model = new ViewModelProvider(this).get(OverviewViewModel.class);
        model.lessons()
                .observe(
                        this,
                        list -> {
                            lessons = list;
                            loaded = true;
                            renderClock();
                        });
        model.motion()
                .observe(
                        this,
                        s -> {
                            b.motionStatus.setText(ProductUi.motion(this, s));
                            b.motionStamp.setText(
                                    getString(
                                            R.string.home_motion_stamp, ProductUi.stamp(this, s)));
                        });
        model.error()
                .observe(
                        this,
                        e -> {
                            b.storageError.setText(e);
                            b.storageError.setVisibility(e.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        b.remindersButton.setOnClickListener(v -> open(RemindersActivity.class));
        b.historyButton.setOnClickListener(v -> open(HistoryActivity.class));
        b.breathingButton.setOnClickListener(v -> open(BreathingActivity.class));
        model.notices()
                .observe(
                        this,
                        notices -> {
                            TankenDatabase.NoticeRow next = null;
                            for (TankenDatabase.NoticeRow notice : notices) {
                                if (next == null
                                        || (notice.nextAttempt == 0
                                                        ? notice.originalDue
                                                        : notice.nextAttempt)
                                                < (next.nextAttempt == 0
                                                        ? next.originalDue
                                                        : next.nextAttempt)) next = notice;
                            }
                            b.nextNotice.setText(
                                    next == null
                                            ? getString(R.string.home_no_notice)
                                            : getString(
                                                    R.string.home_next_notice,
                                                    RecordText.kind(this, next.kind),
                                                    RecordText.date(
                                                            this,
                                                            next.nextAttempt == 0
                                                                    ? next.originalDue
                                                                    : next.nextAttempt)));
                        });
        b.scheduleButton.setOnClickListener(v -> open(ScheduleActivity.class));
        b.motionButton.setOnClickListener(v -> open(MotionActivity.class));
        b.timersButton.setOnClickListener(v -> open(TimersActivity.class));
        b.wellnessButton.setOnClickListener(v -> open(WellnessActivity.class));
        b.button1.setOnClickListener(v -> open(AboutActivity.class));
        b.button2.setOnClickListener(v -> open(SettingsActivity.class));
        b.button3.setOnClickListener(v -> open(TutorialActivity.class));
        b.button5.setOnClickListener(v -> open(CalculatorActivity.class));
        b.button4.setOnClickListener(v -> showGreeting());
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }

    private void showGreeting() {
        BottomSheetInputBinding sheet = BottomSheetInputBinding.inflate(getLayoutInflater());
        greeting = new BottomSheetDialog(this);
        greeting.setContentView(sheet.getRoot());
        sheet.bottomSheetEditText.setOnEditorActionListener(
                (v, action, event) -> {
                    if (action != EditorInfo.IME_ACTION_DONE) return false;
                    greeting.dismiss();
                    Toast.makeText(
                                    this,
                                    getString(R.string.greeting_message, v.getText()),
                                    Toast.LENGTH_LONG)
                            .show();
                    return true;
                });
        greeting.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clock.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onPause() {
        clock.removeCallbacks(tick);
        super.onPause();
    }

    private void renderClock() {
        ZonedDateTime now = ZonedDateTime.now();
        Locale locale = Locale.getDefault();
        b.digitalClock.setText(
                now.format(
                        DateTimeFormatter.ofPattern(
                                android.text.format.DateFormat.is24HourFormat(this)
                                        ? "HH:mm:ss"
                                        : "hh:mm:ss a",
                                locale)));
        if (!loaded) {
            b.academicContext.setText(R.string.schedule_loading);
            return;
        }
        AcademicSchedule.Status status = AcademicSchedule.at(lessons, now);
        StringBuilder text =
                new StringBuilder(
                                now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)))
                        .append("\n")
                        .append(status.dayState);
        if (status.current != null)
            text.append("\n")
                    .append(
                            getString(
                                    R.string.home_current,
                                    status.current.lesson.name,
                                    remaining(now, status.current.end)));
        if (status.next != null)
            text.append("\n")
                    .append(
                            getString(
                                    R.string.home_next,
                                    status.next.lesson.name,
                                    status.next.start.format(
                                            DateTimeFormatter.ofPattern("EEE HH:mm", locale)),
                                    remaining(now, status.next.start)));
        b.academicContext.setText(text.toString());
    }

    private String remaining(ZonedDateTime now, ZonedDateTime target) {
        long s = Math.max(0, (Duration.between(now, target).toMillis() + 999) / 1000);
        String time =
                String.format(
                        Locale.getDefault(), "%02d:%02d:%02d", s / 3600 % 24, s / 60 % 60, s % 60);
        return s >= 86400 ? getString(R.string.home_days, s / 86400, time) : time;
    }

    @Override
    protected void onDestroy() {
        if (greeting != null) greeting.dismiss();
        b = null;
        super.onDestroy();
    }
}
