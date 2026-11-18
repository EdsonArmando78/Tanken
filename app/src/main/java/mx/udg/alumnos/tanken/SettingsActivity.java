package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.ActivitySettingsBinding;

public final class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding b;
    private SettingsViewModel model;
    private boolean updating;
    private androidx.appcompat.app.AlertDialog confirmation;
    private final NotificationAccess notifications =
            new NotificationAccess(this, () -> this, this::renderNotifications);

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(SettingsViewModel.class);
        b.toolbar.setNavigationOnClickListener(
                v -> {
                    if (!Boolean.TRUE.equals(model.busy().getValue())) finish();
                });
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (!Boolean.TRUE.equals(model.busy().getValue())) finish();
                            }
                        });
        ReminderNotifications.createChannel(this);
        TimerNotifications.createChannel(this);
        model.motion()
                .observe(
                        this,
                        state -> {
                            updating = true;
                            b.reading.setChecked(state.enabled);
                            updating = false;
                            b.readingStatus.setText(ProductUi.motion(this, state));
                        });
        b.reading.setOnCheckedChangeListener(
                (view, checked) -> {
                    if (!updating) model.reading(checked);
                });
        b.reminders.setOnClickListener(
                v -> startActivity(MainActivity.destination(this, "reminders")));
        b.notifications.setOnClickListener(v -> notifications.requestOrSettings());
        b.reminderChannel.setOnClickListener(
                v -> notifications.settings(ReminderNotifications.CHANNEL));
        b.timerChannel.setOnClickListener(v -> notifications.settings("class_timers"));
        b.history.setOnClickListener(v -> startActivity(MainActivity.destination(this, "history")));
        b.help.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        b.reset.setOnClickListener(
                v -> {
                    confirmation =
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.reset_title)
                                    .setMessage(R.string.reset_message)
                                    .setNegativeButton(R.string.schedule_cancel, null)
                                    .setPositiveButton(
                                            R.string.reset_confirm, (d, w) -> model.resetAll())
                                    .show();
                });
        model.busy()
                .observe(
                        this,
                        busy -> {
                            b.reading.setEnabled(!busy);
                            b.reset.setEnabled(!busy);
                            b.reminders.setEnabled(!busy);
                            b.history.setEnabled(!busy);
                            b.notifications.setEnabled(!busy);
                            b.reminderChannel.setEnabled(!busy);
                            b.timerChannel.setEnabled(!busy);
                            b.help.setEnabled(!busy);
                            b.reset.setText(busy ? R.string.reset_busy : R.string.reset_title);
                        });
        model.error().observe(this, message -> showMessage(message));
        model.message().observe(this, this::showMessage);
        model.reset()
                .observe(
                        this,
                        complete -> {
                            if (complete) {
                                model.consumeReset();
                                startActivity(
                                        new Intent(this, MainActivity.class)
                                                .addFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            }
                        });
    }

    private void showMessage(String value) {
        b.message.setText(value);
        b.message.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderNotifications() {
        if (b != null)
            b.notificationStatus.setText(
                    getString(
                            R.string.settings_notification_status,
                            getString(
                                    NotificationAccess.enabled(this, ReminderNotifications.CHANNEL)
                                            ? R.string.notification_enabled
                                            : R.string.publication_blocked),
                            getString(
                                    NotificationAccess.enabled(this, "class_timers")
                                            ? R.string.notification_enabled
                                            : R.string.publication_blocked)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNotifications();
    }

    @Override
    protected void onDestroy() {
        if (confirmation != null) confirmation.dismiss();
        b = null;
        super.onDestroy();
    }
}
