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
import androidx.core.content.ContextCompat;

import java.util.function.Supplier;

/** Registered before the owner's creation; shared by Settings, Reminders and Timers. */
final class NotificationAccess {
    private final Supplier<Activity> activity;
    private final ActivityResultLauncher<String> request;
    private boolean asked;

    NotificationAccess(ActivityResultCaller owner, Supplier<Activity> activity, Runnable changed) {
        this.activity = activity;
        request =
                owner.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
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
                && !asked
                && !Boolean.TRUE.equals(store.notificationAsked().getValue())) {
            asked = true;
            store.markNotificationAsked();
            request.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else settings(null);
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
