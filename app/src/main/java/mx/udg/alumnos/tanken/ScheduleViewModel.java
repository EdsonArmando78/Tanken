package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.*;

import java.time.*;
import java.util.*;

public final class ScheduleViewModel extends AndroidViewModel {
    private final AppStore store;
    private final SavedStateHandle saved;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> formError = new MutableLiveData<>("");

    public ScheduleViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        store = AppStore.get(app);
    }

    public LiveData<List<AcademicSchedule.Lesson>> lessons() {
        return store.lessons();
    }

    public LiveData<String> error() {
        return store.error();
    }

    public LiveData<String> formError() {
        return formError;
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<Boolean> editing() {
        return saved.getLiveData("editing", false);
    }

    public String name() {
        return saved.get("name");
    }

    public int day() {
        Integer value = saved.get("day");
        return value == null ? 1 : value;
    }

    public String time(boolean start) {
        String value = saved.get(start ? "start" : "end");
        return value == null ? (start ? "08:00" : "09:00") : value;
    }

    public void name(String value) {
        saved.set("name", value);
    }

    public void day(int value) {
        saved.set("day", value);
    }

    public void time(boolean start, String value) {
        saved.set(start ? "start" : "end", value);
    }

    public void edit(AcademicSchedule.Lesson l) {
        saved.set("id", l == null ? UUID.randomUUID().toString() : l.id);
        name(l == null ? "" : l.name);
        day(l == null ? 1 : l.day.getValue());
        time(true, l == null ? "08:00" : l.start.toString());
        time(false, l == null ? "09:00" : l.end.toString());
        formError.setValue("");
        saved.set("editing", true);
    }

    public void cancel() {
        if (!Boolean.TRUE.equals(busy.getValue())) saved.set("editing", false);
    }

    public void save() {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        try {
            AcademicSchedule.Lesson lesson =
                    new AcademicSchedule.Lesson(
                            saved.get("id"),
                            name(),
                            DayOfWeek.of(day()),
                            LocalTime.parse(time(true)),
                            LocalTime.parse(time(false)));
            busy.setValue(true);
            formError.setValue("");
            store.saveLesson(
                    lesson,
                    failure -> {
                        busy.setValue(false);
                        if (failure == null) saved.set("editing", false);
                        else formError.setValue(failure);
                    });
        } catch (IllegalArgumentException | DateTimeException invalid) {
            formError.setValue(getApplication().getString(R.string.schedule_invalid));
        }
    }

    public void delete(String id) {
        store.deleteLesson(id);
    }
}
