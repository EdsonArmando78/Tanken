package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.*;

import java.util.List;

/** Shared data exposed with the screen's lifecycle; acquisition is application-owned. */
public final class OverviewViewModel extends AndroidViewModel {
    private final AppStore store;
    private final MotionSource source;

    public OverviewViewModel(Application app) {
        super(app);
        store = AppStore.get(app);
        source = ((TankenApplication) app).motion();
    }

    public LiveData<List<AcademicSchedule.Lesson>> lessons() {
        return store.lessons();
    }

    public LiveData<MotionSource.State> motion() {
        return source.state();
    }

    public LiveData<List<TankenDatabase.NoticeRow>> notices() {
        return Transformations.switchMap(store.lessons(), ignored -> store.openNotices());
    }

    public LiveData<String> error() {
        return store.error();
    }

    public void setReading(boolean enabled) {
        source.setEnabled(enabled);
    }

    public void reset() {
        source.reset();
    }
}
