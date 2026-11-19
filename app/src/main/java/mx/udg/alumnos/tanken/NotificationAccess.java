package mx.udg.alumnos.tanken;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Supplier;

/** Registered before the owner's creation; shared by Settings, Reminders and Timers. */
final class NotificationAccess {
    private final Supplier<Activity> activity;
    private final ActivityResultLauncher<String> request;
    private boolean requesting;
    private androidx.appcompat.app.AlertDialog explanation;

    NotificationAccess(ActivityResultCaller owner, Supplier<Activity> activity, Runnable changed) {
        this.activity = activity;
        request =
                owner.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            requesting = false;
                            changed.run();
                            AppStore.get(activity.get()).reconcileReminders();
                        });
    }

    void requestOrSettings() {
        Activity a = activity.get();
        AppStore store = AppStore.get(a);
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && (!previouslyAsked(a)
                        || ActivityCompat.shouldShowRequestPermissionRationale(
                                a, Manifest.permission.POST_NOTIFICATIONS))) {
            if (requesting) return;
            requesting = true;
            store.markNotificationAsked();
            request.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else settings(null);
    }

    private static boolean previouslyAsked(Context context) {
        return context.getSharedPreferences("wellness_preferences", Context.MODE_PRIVATE)
                        .getBoolean("notificationAsked", false)
                || context.getSharedPreferences("TimersActivity", Context.MODE_PRIVATE)
                        .getBoolean("notificationAsked", false)
                || Boolean.TRUE.equals(AppStore.get(context).notificationAsked().getValue());
    }

    static boolean granted(Context context) {
        return (Build.VERSION.SDK_INT < 33
                        || ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED)
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    void explainBlocked(String channel) {
        if (explanation != null && explanation.isShowing()) return;
        explanation =
                new MaterialAlertDialogBuilder(activity.get())
                        .setTitle(R.string.notification_required_title)
                        .setMessage(R.string.notification_required_message)
                        .setNegativeButton(R.string.notification_later, null)
                        .setPositiveButton(
                                R.string.notification_enable,
                                (dialog, which) -> {
                                    if (granted(activity.get())) settings(channel);
                                    else requestOrSettings();
                                })
                        .create();
        explanation.setOnDismissListener(dialog -> explanation = null);
        explanation.show();
    }

    void dismiss() {
        if (explanation != null) explanation.dismiss();
    }

    void settings(String channel) {
        Activity a = activity.get();
        Intent intent =
                new Intent(
                                channel == null
                                        ? Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        : Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
        if (channel != null) intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
        try {
            a.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(a, R.string.external_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    static boolean enabled(Context context, String channel) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel value = manager.getNotificationChannel(channel);
        return manager.areNotificationsEnabled()
                && value != null
                && value.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
}
