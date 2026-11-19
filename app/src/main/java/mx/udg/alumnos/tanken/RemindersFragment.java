package mx.udg.alumnos.tanken;

import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.timepicker.*;

import mx.udg.alumnos.tanken.databinding.FragmentRemindersBinding;

import java.time.LocalTime;

public final class RemindersFragment extends Fragment {
    private FragmentRemindersBinding b;
    private RemindersViewModel model;
    private boolean initialized;
    private String silenceStart = "22:00", silenceEnd = "07:00";
    private final NotificationAccess permission =
            new NotificationAccess(this, () -> requireActivity(), this::notificationStatus);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        b = FragmentRemindersBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(View content, Bundle state) {
        super.onViewCreated(content, state);
        initialized = false;
        model = new ViewModelProvider(this).get(RemindersViewModel.class);
        setFormEnabled(false);
        b.save.setOnClickListener(
                v -> {
                    if (b.hydration.isChecked() || b.breaks.isChecked() || b.classes.isChecked())
                        warnIfBlocked();
                    model.save(capture());
                });
        b.permission.setOnClickListener(v -> permission.requestOrSettings());
        String[] days = getResources().getStringArray(R.array.schedule_days);
        for (String day : days) {
            Chip chip = new Chip(requireContext());
            chip.setText(day);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setCheckedIconResource(R.drawable.ic_ms_done);
            chip.setChipBackgroundColor(
                    androidx.appcompat.content.res.AppCompatResources.getColorStateList(
                            requireContext(), R.color.day_background));
            chip.setTextColor(
                    androidx.appcompat.content.res.AppCompatResources.getColorStateList(
                            requireContext(), R.color.day_foreground));
            chip.setCheckedIconTint(
                    androidx.appcompat.content.res.AppCompatResources.getColorStateList(
                            requireContext(), R.color.day_foreground));
            chip.setContentDescription(getString(R.string.day_unselected, day));
            chip.setSaveEnabled(false);
            b.days.addView(chip);
            chip.setOnCheckedChangeListener(
                    (view, checked) -> {
                        chip.setContentDescription(
                                getString(
                                        checked ? R.string.day_selected : R.string.day_unselected,
                                        day));
                        persistDraft();
                    });
        }
        android.text.TextWatcher watcher =
                new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int count, int after) {}

                    public void onTextChanged(CharSequence s, int a, int before, int count) {
                        persistDraft();
                    }

                    public void afterTextChanged(android.text.Editable e) {}
                };
        b.hydrationMinutes.addTextChangedListener(watcher);
        b.breakMinutes.addTextChangedListener(watcher);
        b.classLead.addTextChangedListener(watcher);
        b.hydration.setOnCheckedChangeListener((v, checked) -> reminderChanged(checked));
        b.breaks.setOnCheckedChangeListener((v, checked) -> reminderChanged(checked));
        b.classes.setOnCheckedChangeListener((v, checked) -> reminderChanged(checked));
        b.silenceStart.setOnClickListener(v -> pick(true));
        b.silenceEnd.setOnClickListener(v -> pick(false));
        model.preferences()
                .observe(
                        getViewLifecycleOwner(),
                        p -> {
                            if (initialized) return;
                            Bundle draft = model.draft(p);
                            b.hydration.setChecked(draft.getBoolean("hydration"));
                            b.breaks.setChecked(draft.getBoolean("breaks"));
                            b.classes.setChecked(draft.getBoolean("classes"));
                            b.hydrationMinutes.setText(draft.getString("hydrationMinutes"));
                            b.breakMinutes.setText(draft.getString("breakMinutes"));
                            b.classLead.setText(draft.getString("lead"));
                            for (int i = 0; i < b.days.getChildCount(); i++)
                                ((Chip) b.days.getChildAt(i))
                                        .setChecked((draft.getInt("days") & (1 << i)) != 0);
                            silenceStart = draft.getString("silenceStart", "22:00");
                            silenceEnd = draft.getString("silenceEnd", "07:00");
                            updateTimes();
                            initialized = true;
                            setFormEnabled(true);
                        });
        RecordAdapter adapter = new RecordAdapter(row -> model.show(row.category, row.id));
        b.records.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.records.setAdapter(adapter);
        model.rows()
                .observe(
                        getViewLifecycleOwner(),
                        rows -> {
                            adapter.submitList(rows);
                            notificationStatus();
                            b.empty.setText(R.string.pending_empty);
                            b.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        });
        model.message()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            b.message.setText(value);
                            b.message.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.error()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            b.storageError.setText(value);
                            b.storageError.setVisibility(
                                    value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.busy()
                .observe(
                        getViewLifecycleOwner(),
                        busy -> {
                            setFormEnabled(initialized && !busy);
                            b.save.setText(busy ? R.string.save_busy : R.string.schedule_save);
                        });
        RecordDialogs.observe(this, model);
        attachPicker(
                (MaterialTimePicker) getChildFragmentManager().findFragmentByTag("silenceStart"),
                true);
        attachPicker(
                (MaterialTimePicker) getChildFragmentManager().findFragmentByTag("silenceEnd"),
                false);
        getParentFragmentManager()
                .setFragmentResultListener(
                        "openNotice",
                        getViewLifecycleOwner(),
                        (key, args) -> model.show("NOTICE", args.getString("id")));
    }

    private void setFormEnabled(boolean enabled) {
        b.save.setEnabled(enabled);
        b.hydration.setEnabled(enabled);
        b.breaks.setEnabled(enabled);
        b.classes.setEnabled(enabled);
        b.hydrationMinutes.setEnabled(enabled);
        b.breakMinutes.setEnabled(enabled);
        b.classLead.setEnabled(enabled);
        b.silenceStart.setEnabled(enabled);
        b.silenceEnd.setEnabled(enabled);
        for (int i = 0; i < b.days.getChildCount(); i++) b.days.getChildAt(i).setEnabled(enabled);
    }

    private void notificationStatus() {
        if (b == null) return;
        boolean blocked = !ReminderNotifications.restriction(requireContext()).isEmpty();
        b.notificationStatus.setText(R.string.notification_required_message);
        b.notificationStatus.setVisibility(blocked ? View.VISIBLE : View.GONE);
        b.permission.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    private void reminderChanged(boolean checked) {
        persistDraft();
        if (initialized && checked) warnIfBlocked();
    }

    private void warnIfBlocked() {
        if (!ReminderNotifications.restriction(requireContext()).isEmpty())
            permission.explainBlocked(ReminderNotifications.CHANNEL);
    }

    private Bundle capture() {
        Bundle value = new Bundle();
        value.putBoolean("hydration", b.hydration.isChecked());
        value.putBoolean("breaks", b.breaks.isChecked());
        value.putBoolean("classes", b.classes.isChecked());
        value.putString("hydrationMinutes", b.hydrationMinutes.getText().toString());
        value.putString("breakMinutes", b.breakMinutes.getText().toString());
        value.putString("lead", b.classLead.getText().toString());
        int days = 0;
        for (int i = 0; i < b.days.getChildCount(); i++)
            if (((Chip) b.days.getChildAt(i)).isChecked()) days |= 1 << i;
        value.putInt("days", days);
        value.putString("silenceStart", silenceStart);
        value.putString("silenceEnd", silenceEnd);
        return value;
    }

    private void persistDraft() {
        if (initialized) model.draft(capture());
    }

    private void updateTimes() {
        b.silenceStart.setText(getString(R.string.silence_start, silenceStart));
        b.silenceEnd.setText(getString(R.string.silence_end, silenceEnd));
    }

    private void pick(boolean start) {
        LocalTime time = LocalTime.parse(start ? silenceStart : silenceEnd);
        MaterialTimePicker picker =
                new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(time.getHour())
                        .setMinute(time.getMinute())
                        .build();
        attachPicker(picker, start);
        picker.show(getChildFragmentManager(), start ? "silenceStart" : "silenceEnd");
    }

    private void attachPicker(MaterialTimePicker picker, boolean start) {
        if (picker == null) return;
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(
                v -> {
                    String time = LocalTime.of(picker.getHour(), picker.getMinute()).toString();
                    if (start) silenceStart = time;
                    else silenceEnd = time;
                    updateTimes();
                    persistDraft();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        notificationStatus();
        AppStore.get(requireContext()).reconcileReminders();
    }

    @Override
    public void onDestroyView() {
        permission.dismiss();
        persistDraft();
        b = null;
        initialized = false;
        super.onDestroyView();
    }
}
