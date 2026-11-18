package mx.udg.alumnos.tanken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.timepicker.*;

import mx.udg.alumnos.tanken.databinding.ActivityRemindersBinding;

import java.time.LocalTime;

public final class RemindersActivity extends AppCompatActivity {
    private ActivityRemindersBinding b;
    private RemindersViewModel model;
    private boolean initialized, permissionAsked;
    private String silenceStart = "22:00", silenceEnd = "07:00";
    private final ActivityResultLauncher<String> permission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        notificationStatus();
                        AppStore.get(this).reconcileReminders();
                    });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityRemindersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(RemindersViewModel.class);
        permissionAsked = state != null && state.getBoolean("permissionAsked");
        b.toolbar.setNavigationOnClickListener(v -> finish());
        setFormEnabled(false);
        b.save.setOnClickListener(v -> model.save(capture()));
        b.permission.setOnClickListener(
                v -> {
                    if (Build.VERSION.SDK_INT >= 33
                            && ContextCompat.checkSelfPermission(
                                            this, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED
                            && !permissionAsked) {
                        permissionAsked = true;
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else
                        startActivity(
                                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                });
        String[] days = getResources().getStringArray(R.array.schedule_days);
        for (String day : days) {
            Chip chip = new Chip(this);
            chip.setText(day);
            chip.setCheckable(true);
            chip.setSaveEnabled(false);
            b.days.addView(chip);
            chip.setOnCheckedChangeListener((view, checked) -> persistDraft());
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
        b.hydration.setOnCheckedChangeListener((v, checked) -> persistDraft());
        b.breaks.setOnCheckedChangeListener((v, checked) -> persistDraft());
        b.classes.setOnCheckedChangeListener((v, checked) -> persistDraft());
        b.silenceStart.setOnClickListener(v -> pick(true));
        b.silenceEnd.setOnClickListener(v -> pick(false));
        model.preferences()
                .observe(
                        this,
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
        b.records.setLayoutManager(new LinearLayoutManager(this));
        b.records.setAdapter(adapter);
        model.rows()
                .observe(
                        this,
                        rows -> {
                            adapter.submitList(rows);
                            notificationStatus();
                            b.empty.setText(R.string.pending_empty);
                            b.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        });
        model.message()
                .observe(
                        this,
                        value -> {
                            b.message.setText(value);
                            b.message.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.error()
                .observe(
                        this,
                        value -> {
                            b.storageError.setText(value);
                            b.storageError.setVisibility(
                                    value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.busy()
                .observe(
                        this,
                        busy -> {
                            setFormEnabled(initialized && !busy);
                            b.save.setText(busy ? R.string.save_busy : R.string.schedule_save);
                        });
        RecordDialogs.observe(this, model);
        attachPicker(
                (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag("silenceStart"),
                true);
        attachPicker(
                (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag("silenceEnd"),
                false);
        if (state == null) openIntent(getIntent());
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
        if (b != null)
            b.notificationStatus.setText(
                    ReminderNotifications.restriction(this).isEmpty()
                            ? R.string.notification_enabled
                            : R.string.publication_blocked);
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
        picker.show(getSupportFragmentManager(), start ? "silenceStart" : "silenceEnd");
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

    private void openIntent(Intent intent) {
        if (intent.getData() != null) model.show("NOTICE", intent.getData().getLastPathSegment());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationStatus();
        AppStore.get(this).reconcileReminders();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        persistDraft();
        state.putBoolean("permissionAsked", permissionAsked);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
