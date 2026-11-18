package mx.udg.alumnos.tanken;

import android.content.Context;

/** Independent of permission grants: declining notifications still completes the welcome. */
final class OnboardingState {
    static final String PREFERENCES = "onboarding";

    private OnboardingState() {}

    static boolean completed(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean("completed", false);
    }

    static void complete(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("completed", true)
                .apply();
    }
}
