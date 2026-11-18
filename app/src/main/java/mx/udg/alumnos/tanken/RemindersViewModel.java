package mx.udg.alumnos.tanken;

import android.app.Application;
import android.os.Bundle;

import androidx.lifecycle.*;

import java.time.LocalTime;
import java.util.*;

public final class RemindersViewModel extends RecordsViewModel {
    private final LiveData<List<HistoryRow>> rows;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>("");

    public RemindersViewModel(Application app, SavedStateHandle saved) {
        super(app, saved);
        rows =
                Transformations.switchMap(
                        store.lessons(),
                        ignored ->
                                Transformations.map(
                                        store.openNotices(),
                                        notices -> {
                                            List<HistoryRow> result = new ArrayList<>();
                                            for (TankenDatabase.NoticeRow n : notices) {
                                                HistoryRow row = new HistoryRow();
                                                row.id = n.id;
                                                row.category = "NOTICE";
                                                row.kind = n.kind;
                                                row.state = n.state;
                                                row.origin = n.origin;
                                                row.at =
                                                        n.nextAttempt == 0
                                                                ? n.originalDue
                                                                : n.nextAttempt;
                                                row.revision = n.updatedAt;
                                                result.add(row);
                                            }
                                            return Collections.unmodifiableList(result);
                                        }));
    }

    public LiveData<List<HistoryRow>> rows() {
        return rows;
    }

    public LiveData<WellnessPreferences> preferences() {
        return store.preferences();
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<String> message() {
        return message;
    }

    public Bundle draft(WellnessPreferences p) {
        Bundle value = saved.get("draft");
        if (value != null) return value;
        value = new Bundle();
        value.putBoolean("hydration", p.hydrationEnabled);
        value.putBoolean("breaks", p.breaksEnabled);
        value.putBoolean("classes", p.classesEnabled);
        value.putString("hydrationMinutes", Integer.toString(p.hydrationMinutes));
        value.putString("breakMinutes", Integer.toString(p.breakMinutes));
        value.putString("lead", Integer.toString(p.classLeadMinutes));
        value.putInt("days", p.dayMask);
        value.putString("silenceStart", p.silenceStart.toString());
        value.putString("silenceEnd", p.silenceEnd.toString());
        saved.set("draft", value);
        return value;
    }

    public void draft(Bundle value) {
        saved.set("draft", new Bundle(value));
    }

    public void save(Bundle value) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        draft(value);
        try {
            WellnessPreferences p =
                    new WellnessPreferences(
                            value.getBoolean("hydration"),
                            value.getBoolean("breaks"),
                            value.getBoolean("classes"),
                            Integer.parseInt(value.getString("hydrationMinutes", "")),
                            Integer.parseInt(value.getString("breakMinutes", "")),
                            Integer.parseInt(value.getString("lead", "")),
                            value.getInt("days"),
                            LocalTime.parse(value.getString("silenceStart", "")),
                            LocalTime.parse(value.getString("silenceEnd", "")));
            busy.setValue(true);
            message.setValue("");
            store.savePreferences(
                    p,
                    failure -> {
                        busy.setValue(false);
                        message.setValue(
                                failure == null
                                        ? getApplication().getString(R.string.preferences_saved)
                                        : failure);
                    });
        } catch (RuntimeException invalid) {
            message.setValue(getApplication().getString(R.string.preferences_invalid));
        }
    }
}
