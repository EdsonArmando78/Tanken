package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.*;
import android.view.*;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.FragmentHomeBinding;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private List<AcademicSchedule.Lesson> lessons = Collections.emptyList();
    private List<TankenDatabase.NoticeRow> notices = Collections.emptyList();
    private boolean loaded, toolsExpanded;
    private String noticeId;
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick =
            new Runnable() {
                @Override
                public void run() {
                    render();
                    clock.postDelayed(
                            this, 60_000 - Math.floorMod(System.currentTimeMillis(), 60_000));
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
        if (state != null) toolsExpanded = state.getBoolean("toolsExpanded", false);
        OverviewViewModel model = new ViewModelProvider(this).get(OverviewViewModel.class);
        model.lessons()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            lessons = list;
                            loaded = true;
                            render();
                        });
        model.notices()
                .observe(
                        getViewLifecycleOwner(),
                        rows -> {
                            notices = rows;
                            render();
                        });
        model.error()
                .observe(
                        getViewLifecycleOwner(),
                        error -> {
                            b.storageError.setText(error);
                            b.storageError.setVisibility(
                                    error.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        b.addSchedule.setOnClickListener(
                v ->
                        startActivity(
                                new Intent(requireContext(), ScheduleActivity.class)
                                        .putExtra("addLesson", true)));
        b.scheduleButton.setOnClickListener(v -> open(ScheduleActivity.class));
        b.remindersButton.setOnClickListener(
                v -> {
                    if (noticeId != null)
                        startActivity(
                                MainActivity.destination(requireContext(), "reminders")
                                        .putExtra("noticeId", noticeId));
                });
        b.breathingButton.setOnClickListener(v -> open(BreathingActivity.class));
        b.timersButton.setOnClickListener(v -> open(TimersActivity.class));
        b.motionButton.setOnClickListener(v -> open(MotionActivity.class));
        b.wellnessButton.setOnClickListener(v -> open(WellnessActivity.class));
        b.toolsToggle.setOnClickListener(
                v -> {
                    toolsExpanded = !toolsExpanded;
                    renderTools();
                });
        for (View row :
                new View[] {
                    b.remindersButton,
                    b.breathingButton,
                    b.toolsToggle,
                    b.timersButton,
                    b.motionButton,
                    b.wellnessButton
                }) {
            ViewCompat.setScreenReaderFocusable(row, true);
            // The row is one action; its label is supplied by the text child.
            ViewCompat.setAccessibilityDelegate(
                    row,
                    new androidx.core.view.AccessibilityDelegateCompat() {
                        @Override
                        public void onInitializeAccessibilityNodeInfo(
                                View host,
                                androidx.core.view.accessibility.AccessibilityNodeInfoCompat info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.setClassName(android.widget.Button.class.getName());
                        }
                    });
        }
        ViewCompat.setAccessibilityHeading(b.className, true);
        renderTools();
        render();
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(requireContext(), screen));
    }

    private void renderTools() {
        if (b == null) return;
        b.toolsContent.setVisibility(toolsExpanded ? View.VISIBLE : View.GONE);
        b.toolsToggleArrow.setRotation(toolsExpanded ? 180 : 0);
        ViewCompat.setStateDescription(
                b.toolsToggle,
                getString(
                        toolsExpanded
                                ? R.string.home_tools_expanded
                                : R.string.home_tools_collapsed));
    }

    private void render() {
        if (b == null) return;
        ZonedDateTime now = ZonedDateTime.now();
        Locale locale = Locale.getDefault();
        String date =
                now.format(
                        DateTimeFormatter.ofPattern(
                                android.text.format.DateFormat.getBestDateTimePattern(
                                        locale, "EEEEdMMMM"),
                                locale));
        b.date.setText(date);
        if (loaded) {
            boolean empty = lessons.isEmpty();
            b.addSchedule.setVisibility(empty ? View.VISIBLE : View.GONE);
            b.scheduleButton.setVisibility(empty ? View.GONE : View.VISIBLE);
            AcademicSchedule.Occurrence next = HomeSummary.lesson(lessons, now);
            b.classLabel.setVisibility(next == null ? View.GONE : View.VISIBLE);
            b.classRoom.setVisibility(
                    next != null && !next.lesson.room.isEmpty() ? View.VISIBLE : View.GONE);
            if (empty) {
                b.className.setText(R.string.home_empty_title);
                b.classDetails.setText(R.string.home_empty_body);
                b.classDetails.setVisibility(View.VISIBLE);
            } else if (next == null) {
                b.className.setText(R.string.home_day_done);
                b.classDetails.setVisibility(View.GONE);
            } else {
                boolean current = !next.start.isAfter(now);
                b.classLabel.setText(
                        current ? R.string.home_current_label : R.string.home_next_label);
                b.className.setText(next.lesson.name);
                b.classRoom.setText(getString(R.string.schedule_room, next.lesson.room));
                long minutes =
                        Math.max(
                                1,
                                (Duration.between(now, current ? next.end : next.start).getSeconds()
                                                + 59)
                                        / 60);
                b.classDetails.setText(
                        getString(
                                current ? R.string.home_class_ends : R.string.home_class_starts,
                                time(current ? next.end : next.start),
                                getResources()
                                        .getQuantityString(
                                                R.plurals.home_minutes, (int) minutes, minutes)));
                b.classDetails.setVisibility(View.VISIBLE);
            }
        }
        TankenDatabase.NoticeRow notice = HomeSummary.notice(notices, now);
        noticeId = notice == null ? null : notice.id;
        b.remindersButton.setVisibility(notice == null ? View.GONE : View.VISIBLE);
        if (notice != null) {
            String title =
                    notice.kind.equals("HYDRATION")
                            ? getString(R.string.home_drink)
                            : notice.kind.equals("ACTIVE_BREAK")
                                    ? getString(R.string.home_take_break)
                                    : notice.kind.equals("CLASS")
                                            ? getString(
                                                    R.string.home_class_notice, notice.lessonName)
                                            : RecordText.kind(requireContext(), notice.kind);
            b.nextNotice.setText(
                    getString(
                            R.string.home_notice_row,
                            title,
                            time(
                                    Instant.ofEpochMilli(HomeSummary.due(notice))
                                            .atZone(now.getZone()))));
        }
    }

    private String time(ZonedDateTime at) {
        Locale locale = Locale.getDefault();
        return at.format(
                DateTimeFormatter.ofPattern(
                        android.text.format.DateFormat.getBestDateTimePattern(
                                locale,
                                android.text.format.DateFormat.is24HourFormat(requireContext())
                                        ? "Hm"
                                        : "hm"),
                        locale));
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

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("toolsExpanded", toolsExpanded);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onDestroyView() {
        clock.removeCallbacks(tick);
        b = null;
        super.onDestroyView();
    }
}
