package mx.udg.alumnos.tanken;

import android.app.Application;
import android.os.Bundle;

import androidx.lifecycle.*;

import java.time.*;
import java.util.List;

public final class HistoryViewModel extends RecordsViewModel {
    private final LiveData<List<HistoryRow>> rows;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>("");

    public HistoryViewModel(Application app, SavedStateHandle saved) {
        super(app, saved);
        rows =
                Transformations.switchMap(
                        store.lessons(),
                        ignored ->
                                Transformations.switchMap(
                                        saved.<Bundle>getLiveData("filter", filter()),
                                        f ->
                                                store.history(
                                                        from(f),
                                                        until(f),
                                                        f.getString("category", "ALL"))));
    }

    public Bundle filter() {
        Bundle value = saved.get("filter");
        if (value != null) return value;
        value = new Bundle();
        value.putString("from", LocalDate.now().minusDays(29).toString());
        value.putString("to", LocalDate.now().toString());
        value.putString("category", "ALL");
        return value;
    }

    private long from(Bundle f) {
        return LocalDate.parse(f.getString("from"))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private long until(Bundle f) {
        return LocalDate.parse(f.getString("to"))
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    public void filter(String from, String to, String category) {
        Bundle value = new Bundle();
        value.putString("from", from);
        value.putString("to", to);
        value.putString("category", category);
        saved.set("filter", value);
    }

    public LiveData<List<HistoryRow>> rows() {
        return rows;
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<String> message() {
        return message;
    }

    public void clear(boolean everything) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        Bundle f = filter();
        busy.setValue(true);
        store.clearHistory(
                from(f),
                until(f),
                f.getString("category", "ALL"),
                everything,
                failure -> {
                    busy.setValue(false);
                    message.setValue(
                            failure == null
                                    ? getApplication().getString(R.string.history_deleted)
                                    : failure);
                });
    }
}
