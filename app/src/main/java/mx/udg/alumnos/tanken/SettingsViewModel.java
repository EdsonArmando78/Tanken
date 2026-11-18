package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.*;

public final class SettingsViewModel extends AndroidViewModel {
    private final AppStore store;
    private final MotionSource motion;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false),
            reset = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>("");
    private boolean closed;

    public SettingsViewModel(Application app) {
        super(app);
        store = AppStore.get(app);
        motion = ((TankenApplication) app).motion();
    }

    public LiveData<MotionSource.State> motion() {
        return motion.state();
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<Boolean> reset() {
        return reset;
    }

    public LiveData<String> error() {
        return store.error();
    }

    public LiveData<String> message() {
        return message;
    }

    public void reading(boolean enabled) {
        motion.setEnabled(enabled);
    }

    public void resetAll() {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        store.resetAll(
                error -> {
                    if (!closed) {
                        busy.setValue(false);
                        if (error == null) reset.setValue(true);
                        else message.setValue(error);
                    }
                });
    }

    public void consumeReset() {
        reset.setValue(false);
    }

    @Override
    protected void onCleared() {
        closed = true;
    }
}
