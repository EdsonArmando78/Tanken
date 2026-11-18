package mx.udg.alumnos.tanken;

import android.content.*;

/** Short serialized work survives Activity destruction; every goAsync result is finished. */
public final class ReminderReceiver extends BroadcastReceiver {
    static final String RESPOND = "mx.udg.alumnos.tanken.RESPOND";
    static final String DUE = "mx.udg.alumnos.tanken.DUE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!RESPOND.equals(action)
                && !DUE.equals(action)
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        PendingResult pending = goAsync();
        AppStore.get(context).handleReminderIntent(intent, pending::finish);
    }
}
