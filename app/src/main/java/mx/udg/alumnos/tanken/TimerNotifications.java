package mx.udg.alumnos.tanken;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

final class TimerNotifications {
    private static final String CHANNEL = "class_timers";
    private TimerNotifications() { }

    static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL, context.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.timer_channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    static void show(Context context, int id, String name) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return;
        Intent intent = new Intent(context, TimersActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            // A unique tag prevents a later screen session from replacing an older notice.
            NotificationManagerCompat.from(context).notify("timer-" + System.nanoTime(), id,
                    new NotificationCompat.Builder(context, CHANNEL)
                            .setSmallIcon(R.drawable.ic_timer_notification)
                            .setContentTitle(context.getString(R.string.timer_finished))
                            .setContentText(name)
                            .setContentIntent(open)
                            .setAutoCancel(true)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .build());
        } catch (SecurityException ignored) {
            // Permission can be revoked between the check and notify; the UI still finishes.
        }
    }
}
