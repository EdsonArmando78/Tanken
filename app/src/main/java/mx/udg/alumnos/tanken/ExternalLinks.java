package mx.udg.alumnos.tanken;

import android.content.*;
import android.net.*;
import android.widget.Toast;

final class ExternalLinks {
    private ExternalLinks() {}

    static void open(Context context, Intent intent, boolean internet) {
        if (internet) {
            ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
            NetworkCapabilities network =
                    manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (network == null
                    || !network.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                Toast.makeText(context, R.string.external_offline, Toast.LENGTH_LONG).show();
                return;
            }
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(context, R.string.external_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
