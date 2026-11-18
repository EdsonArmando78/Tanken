package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.*;

import java.time.LocalTime;

public class ScheduleActivity extends AppCompatActivity {
    private ActivityScheduleBinding b;
    private DialogLessonBinding form;
    private AlertDialog dialog;
    private ScheduleViewModel model;
    private boolean addOnLoad;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        b = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        model = new ViewModelProvider(this).get(ScheduleViewModel.class);
        addOnLoad =
                state == null
                        ? getIntent().getBooleanExtra("addLesson", false)
                        : state.getBoolean("addOnLoad", false);
        LessonAdapter adapter =
                new LessonAdapter(
                        model::edit,
                        l ->
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle(getString(R.string.schedule_delete_title, l.name))
                                        .setMessage(R.string.schedule_delete_group)
                                        .setNegativeButton(R.string.schedule_cancel, null)
                                        .setPositiveButton(
                                                R.string.schedule_delete,
                                                (d, w) -> model.delete(l.groupId))
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
                            adapter.submitList(LessonGroup.from(list));
                            b.empty.setText(R.string.schedule_empty);
                            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                            b.add.setEnabled(true);
                            if (addOnLoad) {
                                addOnLoad = false;
                                model.edit(null);
                            }
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
        getSupportFragmentManager()
                .setFragmentResultListener(
                        LessonTimePicker.RESULT,
                        this,
                        (key, result) -> {
                            model.time(
                                    result.getBoolean("start"),
                                    LocalTime.of(result.getInt("hour"), result.getInt("minute"))
                                            .toString());
                            updateTimes();
                        });
    }

    private void showEditor() {
        form = DialogLessonBinding.inflate(getLayoutInflater());
        form.name.setText(model.name());
        form.room.setText(model.room());
        form.room.addTextChangedListener(
                new android.text.TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        model.room(s.toString());
                    }

                    public void afterTextChanged(android.text.Editable value) {}
                });
        form.name.addTextChangedListener(
                new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

                    public void onTextChanged(CharSequence s, int a, int before, int count) {
                        model.name(s.toString());
                    }

                    public void afterTextChanged(android.text.Editable e) {}
                });
        String[] labels = getResources().getStringArray(R.array.schedule_days);
        for (int i = 0; i < form.days.getChildCount(); i++) {
            View button = form.days.getChildAt(i);
            button.setContentDescription(labels[i]);
            // The ViewModel owns the draft, including after rotation.
            button.setSaveEnabled(false);
        }
        form.days.setSaveEnabled(false);
        for (int i = 0; i < form.days.getChildCount(); i++)
            if ((model.days() & (1 << i)) != 0) form.days.check(form.days.getChildAt(i).getId());
        form.dayHint.setVisibility((model.days() & ~63) != 0 ? View.VISIBLE : View.GONE);
        form.days.addOnButtonCheckedListener(
                (group, checkedId, checked) -> {
                    for (int i = 0; i < group.getChildCount(); i++)
                        if (group.getChildAt(i).getId() == checkedId)
                            model.selectedDay(i + 1, checked);
                    form.dayHint.setVisibility(View.GONE);
                });
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
        form.room.setEnabled(!busy);
        for (int i = 0; i < form.days.getChildCount(); i++)
            form.days.getChildAt(i).setEnabled(!busy);
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
        String tag = start ? "lessonStart" : "lessonEnd";
        if (getSupportFragmentManager().isStateSaved()
                || getSupportFragmentManager().findFragmentByTag(tag) != null) return;
        LessonTimePicker.create(start, model.time(start)).show(getSupportFragmentManager(), tag);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("addOnLoad", addOnLoad);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) dialog.dismiss();
        b = null;
        super.onDestroy();
    }
}
