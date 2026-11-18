package mx.udg.alumnos.tanken;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.DialogTimerDurationBinding;

public final class TimerDurationDialog extends DialogFragment {
    static final String RESULT = "timerDuration";
    private DialogTimerDurationBinding b;

    static TimerDurationDialog create(long seconds) {
        Bundle args = new Bundle();
        args.putLong("seconds", seconds);
        TimerDurationDialog fragment = new TimerDurationDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle saved) {
        b = DialogTimerDurationBinding.inflate(getLayoutInflater());
        long seconds = requireArguments().getLong("seconds", 60);
        b.hours.setText(
                saved == null
                        ? String.format(java.util.Locale.getDefault(), "%d", seconds / 3600)
                        : saved.getString("hours", "0"));
        b.minutes.setText(
                saved == null
                        ? String.format(java.util.Locale.getDefault(), "%d", seconds / 60 % 60)
                        : saved.getString("minutes", "0"));
        b.seconds.setText(
                saved == null
                        ? String.format(java.util.Locale.getDefault(), "%d", seconds % 60)
                        : saved.getString("seconds", "0"));
        android.text.TextWatcher watcher =
                new android.text.TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        preview();
                    }

                    public void afterTextChanged(android.text.Editable text) {}
                };
        b.hours.addTextChangedListener(watcher);
        b.minutes.addTextChangedListener(watcher);
        b.seconds.addTextChangedListener(watcher);
        b.fiveMinutes.setOnClickListener(v -> preset(300));
        b.fifteenMinutes.setOnClickListener(v -> preset(900));
        b.thirtyMinutes.setOnClickListener(v -> preset(1800));
        b.oneHour.setOnClickListener(v -> preset(3600));
        b.seconds.setOnEditorActionListener(
                (v, action, event) -> {
                    if (action != EditorInfo.IME_ACTION_DONE) return false;
                    confirm();
                    return true;
                });
        preview();
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.timer_duration_label)
                .setView(b.getRoot())
                .setNegativeButton(R.string.schedule_cancel, null)
                .setPositiveButton(R.string.timer_duration_apply, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> confirm());
        if (dialog.getWindow() != null)
            dialog.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void preset(long seconds) {
        b.hours.setText(String.format(java.util.Locale.getDefault(), "%d", seconds / 3600));
        b.minutes.setText(String.format(java.util.Locale.getDefault(), "%d", seconds / 60 % 60));
        b.seconds.setText(String.format(java.util.Locale.getDefault(), "%d", seconds % 60));
    }

    private long value() {
        return TimerDuration.seconds(
                b.hours.getText().toString(),
                b.minutes.getText().toString(),
                b.seconds.getText().toString());
    }

    private void preview() {
        if (b == null) return;
        b.error.setVisibility(View.GONE);
        try {
            b.preview.setText(TimerDuration.label(requireContext(), value()));
        } catch (IllegalArgumentException invalid) {
            b.preview.setText(R.string.timer_duration_help);
        }
    }

    private void confirm() {
        try {
            Bundle result = new Bundle();
            result.putLong("seconds", value());
            getParentFragmentManager().setFragmentResult(RESULT, result);
            dismiss();
        } catch (IllegalArgumentException invalid) {
            b.error.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle saved) {
        saved.putString("hours", b.hours.getText().toString());
        saved.putString("minutes", b.minutes.getText().toString());
        saved.putString("seconds", b.seconds.getText().toString());
        super.onSaveInstanceState(saved);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
