package mx.udg.alumnos.tanken;

import android.content.Context;
import android.net.Uri;
import android.os.*;

import androidx.lifecycle.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Shares AppStore's writer and database; failed imports never replace the active catalog. */
public final class CatalogRepository {
    private final Context context;
    private final TankenDatabase db;
    private final Executor io;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Catalog> catalog = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>("");

    CatalogRepository(Context context, TankenDatabase db, Executor io) {
        this.context = context;
        this.db = db;
        this.io = io;
    }

    public LiveData<Catalog> catalog() {
        return catalog;
    }

    public LiveData<String> error() {
        return error;
    }

    Catalog bundled() throws IOException {
        try (InputStream stream = context.getAssets().open("services.json")) {
            return CatalogCodec.read(stream);
        }
    }

    void initialize() {
        try {
            TankenDatabase.CatalogInfo info = db.records().catalogInfo();
            if (info == null) replace(bundled(), false);
            else {
                List<Catalog.Service> services = new ArrayList<>();
                for (TankenDatabase.ServiceRow row : db.records().services())
                    services.add(row.service());
                catalog.postValue(
                        new Catalog(info.version, services, info.imported, info.importedAt));
            }
            error.postValue("");
        } catch (IOException | RuntimeException failed) {
            error.postValue(context.getString(R.string.catalog_load_failed));
        }
    }

    public void preview(Uri uri, Consumer<Catalog> done) {
        io.execute(
                () -> {
                    Catalog result = null;
                    try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
                        if (stream == null) throw new IOException("Missing file");
                        result = CatalogCodec.read(stream);
                        error.postValue("");
                    } catch (IOException | RuntimeException invalid) {
                        error.postValue(context.getString(R.string.catalog_import_failed));
                    }
                    Catalog value = result;
                    main.post(() -> done.accept(value));
                });
    }

    public void apply(Catalog value, Consumer<Boolean> done) {
        io.execute(
                () -> {
                    boolean success = false;
                    try {
                        replace(value, true);
                        error.postValue("");
                        success = true;
                    } catch (RuntimeException failed) {
                        error.postValue(context.getString(R.string.catalog_import_failed));
                    }
                    boolean result = success;
                    main.post(() -> done.accept(result));
                });
    }

    void replace(Catalog value, boolean imported) {
        long at = imported ? System.currentTimeMillis() : 0;
        persist(db, value, imported, at);
        catalog.postValue(new Catalog(value.version, value.services, imported, at));
    }

    static void persist(TankenDatabase db, Catalog value, boolean imported, long at) {
        db.runInTransaction(
                () -> {
                    db.records().deleteServices();
                    for (Catalog.Service service : value.services)
                        db.records().putService(new TankenDatabase.ServiceRow(service));
                    TankenDatabase.CatalogInfo info = new TankenDatabase.CatalogInfo();
                    info.version = value.version;
                    info.imported = imported;
                    info.importedAt = at;
                    db.records().putCatalogInfo(info);
                });
    }
}
