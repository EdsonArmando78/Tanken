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
        ProcessLifecycleOwner.get().getLifecycle().addObserver(motion);
    }

    public MotionSource motion() {
        return motion;
    }

    public AppStore store() {
        return store;
    }
}
