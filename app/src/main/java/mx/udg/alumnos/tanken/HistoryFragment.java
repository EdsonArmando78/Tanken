package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.*;
import android.view.View;

import androidx.core.util.Pair;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.FragmentHistoryBinding;

import java.time.*;

public final class HistoryFragment extends Fragment {
    private FragmentHistoryBinding b;
    private HistoryViewModel model;
    private static final String[] CATEGORIES = {"ALL", "NOTICE", "MOTION", "COMPARISON"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        b = FragmentHistoryBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(View content, Bundle state) {
        super.onViewCreated(content, state);
        model = new ViewModelProvider(this).get(HistoryViewModel.class);
        ((MenuHost) requireActivity())
                .addMenuProvider(
                        new MenuProvider() {
                            @Override
                            public void onCreateMenu(Menu menu, MenuInflater inflater) {
                                inflater.inflate(R.menu.history_actions, menu);
                            }

                            @Override
                            public void onPrepareMenu(Menu menu) {
                                boolean enabled = !Boolean.TRUE.equals(model.busy().getValue());
                                menu.findItem(R.id.clearFilteredHistory).setEnabled(enabled);
                                menu.findItem(R.id.clearAllHistory).setEnabled(enabled);
                            }

                            @Override
                            public boolean onMenuItemSelected(MenuItem item) {
                                if (item.getItemId() != R.id.clearFilteredHistory
                                        && item.getItemId() != R.id.clearAllHistory) return false;
                                confirmClear(item.getItemId() == R.id.clearAllHistory);
                                return true;
                            }
                        },
                        getViewLifecycleOwner(),
                        Lifecycle.State.RESUMED);
        RecordAdapter adapter = new RecordAdapter(row -> model.show(row.category, row.id));
        b.records.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.records.setAdapter(adapter);
        model.rows()
                .observe(
                        getViewLifecycleOwner(),
                        rows -> {
                            adapter.submitList(rows);
                            b.empty.setText(R.string.history_empty);
                            b.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        });
        model.error()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            b.storageError.setText(value);
                            b.storageError.setVisibility(
                                    value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.message()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            b.message.setText(value);
                            b.message.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.busy()
                .observe(
                        getViewLifecycleOwner(), busy -> requireActivity().invalidateOptionsMenu());
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
                        getChildFragmentManager().findFragmentByTag("historyDates"));
    }

    private void confirmClear(boolean all) {
        new MaterialAlertDialogBuilder(requireContext())
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
        picker.show(getChildFragmentManager(), "historyDates");
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
    public void onResume() {
        super.onResume();
        Bundle f = model.filter();
        model.filter(f.getString("from"), f.getString("to"), f.getString("category", "ALL"));
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
