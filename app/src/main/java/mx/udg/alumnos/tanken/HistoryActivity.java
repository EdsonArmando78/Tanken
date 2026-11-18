package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.ActivityHistoryBinding;

import java.time.*;

public final class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding b;
    private HistoryViewModel model;
    private static final String[] CATEGORIES = {"ALL", "NOTICE", "MOTION", "COMPARISON"};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(HistoryViewModel.class);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.toolbar.inflateMenu(R.menu.history_actions);
        b.toolbar.setOnMenuItemClickListener(
                item -> {
                    confirmClear(item.getItemId() == R.id.clearAllHistory);
                    return true;
                });
        RecordAdapter adapter = new RecordAdapter(row -> model.show(row.category, row.id));
        b.records.setLayoutManager(new LinearLayoutManager(this));
        b.records.setAdapter(adapter);
        model.rows()
                .observe(
                        this,
                        rows -> {
                            adapter.submitList(rows);
                            b.empty.setText(R.string.history_empty);
                            b.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        });
        model.error()
                .observe(
                        this,
                        value -> {
                            b.storageError.setText(value);
                            b.storageError.setVisibility(
                                    value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.message()
                .observe(
                        this,
                        value -> {
                            b.message.setText(value);
                            b.message.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.busy()
                .observe(
                        this,
                        busy -> {
                            b.toolbar
                                    .getMenu()
                                    .findItem(R.id.clearFilteredHistory)
                                    .setEnabled(!busy);
                            b.toolbar.getMenu().findItem(R.id.clearAllHistory).setEnabled(!busy);
                        });
        String[] types = getResources().getStringArray(R.array.history_types);
        b.type.setSimpleItems(types);
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equals(model.filter().getString("category")))
                b.type.setText(types[i], false);
        b.type.setOnItemClickListener(
                (parent, view, position, id) -> {
                    Bundle f = model.filter();
                    model.filter(f.getString("from"), f.getString("to"), CATEGORIES[position]);
                });
        updateDates();
        b.dates.setOnClickListener(v -> pickDates());
        RecordDialogs.observe(this, model);
        attachPicker(
                (MaterialDatePicker<?>)
                        getSupportFragmentManager().findFragmentByTag("historyDates"));
    }

    private void confirmClear(boolean all) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(all ? R.string.history_delete_all : R.string.history_delete)
                .setMessage(
                        all ? R.string.history_delete_all_message : R.string.history_delete_message)
                .setNegativeButton(R.string.schedule_cancel, null)
                .setPositiveButton(R.string.schedule_delete, (d, w) -> model.clear(all))
                .show();
    }

    private void updateDates() {
        Bundle f = model.filter();
        b.dates.setText(getString(R.string.history_dates, f.getString("from"), f.getString("to")));
    }

    private void pickDates() {
        Bundle f = model.filter();
        long from =
                LocalDate.parse(f.getString("from"))
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli();
        long to =
                LocalDate.parse(f.getString("to"))
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli();
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setSelection(new Pair<>(from, to))
                        .build();
        attachPicker(picker);
        picker.show(getSupportFragmentManager(), "historyDates");
    }

    private void attachPicker(MaterialDatePicker<?> picker) {
        if (picker == null) return;
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(
                selection -> {
                    if (!(selection instanceof Pair)) return;
                    Pair<?, ?> range = (Pair<?, ?>) selection;
                    if (!(range.first instanceof Long) || !(range.second instanceof Long)) return;
                    String from =
                            Instant.ofEpochMilli((Long) range.first)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString();
                    String to =
                            Instant.ofEpochMilli((Long) range.second)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString();
                    model.filter(from, to, model.filter().getString("category", "ALL"));
                    updateDates();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle f = model.filter();
        model.filter(f.getString("from"), f.getString("to"), f.getString("category", "ALL"));
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
