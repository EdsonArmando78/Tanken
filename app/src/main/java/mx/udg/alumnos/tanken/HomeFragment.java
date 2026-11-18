package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        b = FragmentHomeBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        OverviewViewModel model = new ViewModelProvider(this).get(OverviewViewModel.class);
        model.lessons()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            lessons = list;
                            loaded = true;
                            renderClock();
                        });
        model.motion()
                .observe(
                        getViewLifecycleOwner(),
                        s -> {
                            b.motionStatus.setText(ProductUi.motion(requireContext(), s));
                            b.motionStamp.setText(
                                    getString(
                                            R.string.home_motion_stamp,
                                            ProductUi.stamp(requireContext(), s)));
                        });
        model.error()
                .observe(
                        getViewLifecycleOwner(),
                        e -> {
                            b.storageError.setText(e);
                            b.storageError.setVisibility(e.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        b.remindersButton.setOnClickListener(v -> select(R.id.reminders));
        b.breathingButton.setOnClickListener(v -> open(BreathingActivity.class));
        model.notices()
                .observe(
                        getViewLifecycleOwner(),
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
                                                    RecordText.kind(requireContext(), next.kind),
                                                    RecordText.date(
                                                            requireContext(),
                                                            next.nextAttempt == 0
                                                                    ? next.originalDue
                                                                    : next.nextAttempt)));
                        });
        b.scheduleButton.setOnClickListener(v -> open(ScheduleActivity.class));
        b.motionButton.setOnClickListener(v -> open(MotionActivity.class));
        b.timersButton.setOnClickListener(v -> open(TimersActivity.class));
        b.wellnessButton.setOnClickListener(v -> open(WellnessActivity.class));
    }

    private void select(int destination) {
        ((MainActivity) requireActivity()).select(destination);
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(requireContext(), screen));
    }

    @Override
    public void onResume() {
        super.onResume();
        clock.removeCallbacks(tick);
        tick.run();
    }

    @Override
    public void onPause() {
        clock.removeCallbacks(tick);
        super.onPause();
    }

    private void renderClock() {
        ZonedDateTime now = ZonedDateTime.now();
        Locale locale = Locale.getDefault();
        b.digitalClock.setText(
                now.format(
                        DateTimeFormatter.ofPattern(
                                android.text.format.DateFormat.is24HourFormat(requireContext())
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
    public void onDestroyView() {
        clock.removeCallbacks(tick);
        b = null;
        super.onDestroyView();
    }
}
