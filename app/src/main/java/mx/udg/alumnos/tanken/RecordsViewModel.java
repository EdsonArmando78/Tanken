package mx.udg.alumnos.tanken;

import android.app.Application;

import androidx.lifecycle.*;

/** Shared detail/action handling; callbacks target the ViewModel, never an Activity. */
public class RecordsViewModel extends AndroidViewModel {
    protected final AppStore store;
    protected final SavedStateHandle saved;
    private final MutableLiveData<RecordDetail> detail = new MutableLiveData<>();
    private int request;
    private boolean closed;

    public RecordsViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        store = AppStore.get(app);
    }

    public LiveData<RecordDetail> detail() {
        return detail;
    }

    public LiveData<String> error() {
        return store.error();
    }

    public void show(String category, String id) {
        int generation = ++request;
        store.loadDetail(
                category,
                id,
                value -> {
                    if (!closed && generation == request) detail.setValue(value);
                });
    }

    public void dismissDetail() {
        request++;
        detail.setValue(null);
    }

    public void respond(TankenDatabase.NoticeRow row, NoticeRules.State response) {
        store.respondNotice(row, response);
    }

    @Override
    protected void onCleared() {
        closed = true;
        request++;
    }
}
