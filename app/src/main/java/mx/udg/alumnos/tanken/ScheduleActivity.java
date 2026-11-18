package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.*;

import mx.udg.alumnos.tanken.databinding.*;

import java.time.LocalTime;

public class ScheduleActivity extends AppCompatActivity {
    private ActivityScheduleBinding b;
    private DialogLessonBinding form;
    private AlertDialog dialog;
    private ScheduleViewModel model;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(ScheduleViewModel.class);
        LessonAdapter adapter =
                new LessonAdapter(
                        model::edit,
                        l ->
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle(getString(R.string.schedule_delete_title, l.name))
                                        .setNegativeButton(R.string.schedule_cancel, null)
                                        .setPositiveButton(
                                                R.string.schedule_delete,
                                                (d, w) -> model.delete(l.id))
                                        .show());
        b.lessons.setLayoutManager(new LinearLayoutManager(this));
        b.lessons.setAdapter(adapter);
        b.add.setEnabled(false);
        b.add.setOnClickListener(v -> model.edit(null));
        b.toolbar.setNavigationOnClickListener(v -> finish());
        model.lessons()
                .observe(
                        this,
                        list -> {
                            adapter.submitList(list);
                            b.empty.setText(R.string.schedule_empty);
                            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                            b.add.setEnabled(true);
                        });
        model.error()
                .observe(
                        this,
                        error -> {
                            b.storageError.setText(error);
                            b.storageError.setVisibility(
                                    error.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.editing()
                .observe(
                        this,
                        editing -> {
                            if (editing && dialog == null) showEditor();
                            else if (!editing && dialog != null) dialog.dismiss();
                        });
        model.formError()
                .observe(
                        this,
                        error -> {
                            if (form != null) {
                                form.error.setText(error);
                                form.error.setVisibility(
                                        error.isEmpty() ? View.GONE : View.VISIBLE);
                            }
                        });
        model.busy().observe(this, value -> updateBusy(value));
        attachPicker(
                (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag("lessonStart"),
                true);
        attachPicker(
                (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag("lessonEnd"),
                false);
    }

    private void showEditor() {
        form = DialogLessonBinding.inflate(getLayoutInflater());
        form.name.setText(model.name());
        form.name.addTextChangedListener(
                new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

                    public void onTextChanged(CharSequence s, int a, int before, int count) {
                        model.name(s.toString());
                    }

                    public void afterTextChanged(android.text.Editable e) {}
                });
        String[] days = getResources().getStringArray(R.array.schedule_days);
        form.day.setSimpleItems(days);
        form.day.setText(days[model.day() - 1], false);
        form.day.setOnItemClickListener((p, v, pos, id) -> model.day(pos + 1));
        updateTimes();
        form.start.setOnClickListener(v -> pick(true));
        form.end.setOnClickListener(v -> pick(false));
        String error = model.formError().getValue();
        form.error.setText(error);
        form.error.setVisibility(error == null || error.isEmpty() ? View.GONE : View.VISIBLE);
        dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.schedule_edit)
                        .setView(form.getRoot())
                        .setNegativeButton(R.string.schedule_cancel, (d, w) -> model.cancel())
                        .setPositiveButton(R.string.schedule_save, null)
                        .create();
        dialog.setOnCancelListener(d -> model.cancel());
        dialog.setOnDismissListener(
                d -> {
                    dialog = null;
                    form = null;
                });
        dialog.setOnShowListener(
                d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(v -> model.save());
                    updateBusy(Boolean.TRUE.equals(model.busy().getValue()));
                });
        dialog.show();
    }

    private void updateBusy(boolean busy) {
        if (dialog == null || !dialog.isShowing()) return;
        dialog.setCancelable(!busy);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setText(busy ? R.string.save_busy : R.string.schedule_save);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!busy);
        form.name.setEnabled(!busy);
        form.day.setEnabled(!busy);
        form.start.setEnabled(!busy);
        form.end.setEnabled(!busy);
    }

    private void updateTimes() {
        if (form != null) {
            form.start.setText(getString(R.string.schedule_start, model.time(true)));
            form.end.setText(getString(R.string.schedule_end, model.time(false)));
        }
    }

    private void pick(boolean start) {
        LocalTime time = LocalTime.parse(model.time(start));
        MaterialTimePicker picker =
                new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(time.getHour())
                        .setMinute(time.getMinute())
                        .setTitleText(
                                start ? R.string.schedule_start_label : R.string.schedule_end_label)
                        .build();
        attachPicker(picker, start);
        picker.show(getSupportFragmentManager(), start ? "lessonStart" : "lessonEnd");
    }

    private void attachPicker(MaterialTimePicker picker, boolean start) {
        if (picker == null) return;
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(
                v -> {
                    model.time(
                            start, LocalTime.of(picker.getHour(), picker.getMinute()).toString());
                    updateTimes();
                });
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) dialog.dismiss();
        b = null;
        super.onDestroy();
    }
}
