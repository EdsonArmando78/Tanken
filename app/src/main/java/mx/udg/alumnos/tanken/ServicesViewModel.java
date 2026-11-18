package mx.udg.alumnos.tanken;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.*;

public final class ServicesViewModel extends AndroidViewModel {
    private final CatalogRepository repository;
    private final MutableLiveData<Catalog> preview = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>("");
    private boolean closed;

    public ServicesViewModel(Application app) {
        super(app);
        repository = AppStore.get(app).catalog();
    }

    public LiveData<Catalog> catalog() {
        return repository.catalog();
    }

    public LiveData<Catalog> preview() {
        return preview;
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<String> error() {
        return repository.error();
    }

    public LiveData<String> message() {
        return message;
    }

    public void preview(Uri uri) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        repository.preview(
                uri,
                value -> {
                    if (!closed) {
                        busy.setValue(false);
                        preview.setValue(value);
                    }
                });
    }

    public void discardPreview() {
        preview.setValue(null);
    }

    public void confirm() {
        Catalog value = preview.getValue();
        if (value == null || Boolean.TRUE.equals(busy.getValue())) return;
        preview.setValue(null);
        busy.setValue(true);
        repository.apply(
                value,
                success -> {
                    if (!closed) {
                        busy.setValue(false);
                        if (success)
                            message.setValue(getApplication().getString(R.string.catalog_imported));
                    }
                });
    }

    @Override
    protected void onCleared() {
        closed = true;
    }
}
