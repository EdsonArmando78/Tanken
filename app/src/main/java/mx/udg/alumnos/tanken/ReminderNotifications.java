package mx.udg.alumnos.tanken;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.*;
import androidx.core.content.ContextCompat;

final class ReminderNotifications {
    static final String CHANNEL = "wellness_reminders";

    private ReminderNotifications() {}

    static void createChannel(Context c) {
        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL,
                        c.getString(R.string.reminders_title),
                        NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(c.getString(R.string.reminders_channel_description));
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    static String restriction(Context c) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return "BLOCKED_PERMISSION";
        NotificationChannel channel =
                c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()
                || channel == null
                || channel.getImportance() == NotificationManager.IMPORTANCE_NONE)
            return "BLOCKED_CHANNEL";
        return "";
    }

    static String publish(Context c, TankenDatabase.NoticeRow row) {
        String restriction = restriction(c);
        if (!restriction.isEmpty()) return restriction;
        Intent open =
                MainActivity.destination(c, "reminders")
                        .putExtra("noticeId", row.id)
                        .setData(Uri.parse("tanken://notice/" + row.id))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content =
                PendingIntent.getActivity(
                        c,
                        0,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(c, CHANNEL)
                        .setSmallIcon(R.drawable.ic_nav_reminders)
                        .setContentTitle(RecordText.kind(c, row.kind))
                        .setContentText(
                                row.kind.equals("CLASS")
                                        ? row.lessonName
                                        : c.getString(
                                                row.kind.equals("HYDRATION")
                                                        ? R.string.hydration_prompt
                                                        : R.string.break_prompt))
                        .setContentIntent(content)
                        .setOnlyAlertOnce(true)
                        .setTimeoutAfter(Math.max(1, row.expiresAt - System.currentTimeMillis()))
                        .setAutoCancel(false)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .addAction(
                                R.drawable.ic_ms_done,
                                c.getString(R.string.notice_done),
                                action(c, row, "COMPLETED"))
                        .addAction(
                                R.drawable.ic_ms_snooze,
                                c.getString(R.string.notice_snooze),
                                action(c, row, "SNOOZED"))
                        .addAction(
                                R.drawable.ic_ms_skip_next,
                                c.getString(R.string.notice_skip),
                                action(c, row, "SKIPPED"));
        try {
            NotificationManagerCompat.from(c).notify("wellness:" + row.id, 0, notification.build());
            return "PUBLISHED";
        } catch (SecurityException denied) {
            return "BLOCKED_PERMISSION";
        }
    }

    private static PendingIntent action(Context c, TankenDatabase.NoticeRow row, String action) {
        Intent intent =
                new Intent(c, ReminderReceiver.class)
                        .setAction(ReminderReceiver.RESPOND)
                        .setData(
                                Uri.parse(
                                        "tanken://action/"
                                                + row.id
                                                + "/"
                                                + row.revision
                                                + "/"
                                                + action))
                        .putExtra("id", row.id)
                        .putExtra("revision", row.revision)
                        .putExtra("response", action);
        return PendingIntent.getBroadcast(
                c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancel(Context c, String id) {
        NotificationManagerCompat.from(c).cancel("wellness:" + id, 0);
    }

    static void cancelAll(Context c) {
        for (android.service.notification.StatusBarNotification notification :
                c.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (notification.getTag() != null && notification.getTag().startsWith("wellness:"))
                NotificationManagerCompat.from(c)
                        .cancel(notification.getTag(), notification.getId());
        }
    }
}
