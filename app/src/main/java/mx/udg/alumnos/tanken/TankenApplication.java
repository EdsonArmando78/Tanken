package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.material.color.DynamicColors;

public final class TankenApplication extends Application {
    private MotionSource motion;
    private AppStore store;

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        store = new AppStore(this);
        motion = new MotionSource(this);
        store.io.execute(motion::reloadPreferences);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(motion);
        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(
                        new androidx.lifecycle.DefaultLifecycleObserver() {
                            @Override
                            public void onStart(androidx.lifecycle.LifecycleOwner owner) {
                                store.reconcileReminders();
                            }
                        });
    }

    public MotionSource motion() {
        return motion;
    }

    public AppStore store() {
        return store;
    }
}
