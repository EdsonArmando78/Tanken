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

    public String room() {
        String value = saved.get("room");
        return value == null ? "" : value;
    }

    public void room(String value) {
        saved.set("room", value);
    }

    public int days() {
        Integer value = saved.get("days");
        return value == null ? 1 << (day() - 1) : value;
    }

    public void selectedDay(int day, boolean selected) {
        int mask = days() & 63;
        saved.set("days", selected ? mask | (1 << (day - 1)) : mask & ~(1 << (day - 1)));
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
        saved.set("days", 1 << (value - 1));
    }

    public void time(boolean start, String value) {
        saved.set(start ? "start" : "end", value);
    }

    public void edit(AcademicSchedule.Lesson l) {
        saved.set("id", l == null ? UUID.randomUUID().toString() : l.id);
        saved.set("groupId", l == null ? UUID.randomUUID().toString() : l.groupId);
        name(l == null ? "" : l.name);
        room(l == null ? "" : l.room);
        day(l == null ? 1 : l.day.getValue());
        if (l != null) {
            int mask = 0;
            for (AcademicSchedule.Lesson occurrence : store.snapshot())
                if (occurrence.groupId.equals(l.groupId))
                    mask |= 1 << (occurrence.day.getValue() - 1);
            saved.set("days", mask);
        }
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
        if (days() == 0 || (days() & ~63) != 0) {
            formError.setValue(getApplication().getString(R.string.schedule_day_required));
            return;
        }
        try {
            String groupId = saved.get("groupId");
            if (groupId == null) {
                groupId = saved.get("id");
                if (groupId == null) groupId = UUID.randomUUID().toString();
                saved.set("groupId", groupId);
            }
            List<AcademicSchedule.Lesson> changes = new ArrayList<>();
            for (int day = 1; day <= 6; day++) {
                if ((days() & (1 << (day - 1))) == 0) continue;
                String id =
                        UUID.nameUUIDFromBytes(
                                        (groupId + ":" + day)
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .toString();
                for (AcademicSchedule.Lesson existing : store.snapshot())
                    if (existing.groupId.equals(groupId) && existing.day.getValue() == day)
                        id = existing.id;
                changes.add(
                        new AcademicSchedule.Lesson(
                                id,
                                name(),
                                DayOfWeek.of(day),
                                LocalTime.parse(time(true)),
                                LocalTime.parse(time(false)),
                                groupId,
                                room()));
            }
            busy.setValue(true);
            formError.setValue("");
            store.saveLessonGroup(
                    groupId,
                    changes,
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
        store.deleteLessonGroup(id);
    }
}
