package mx.udg.alumnos.tanken;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/** Compatibility facade. load() only reads an immutable in-memory snapshot. */
public final class ScheduleRepository {
    private final AppStore store;

    public ScheduleRepository(Context context) {
        store = AppStore.get(context);
    }

    public List<AcademicSchedule.Lesson> load() {
        return store.snapshot();
    }

    public LiveData<List<AcademicSchedule.Lesson>> observe() {
        return store.lessons();
    }
}
