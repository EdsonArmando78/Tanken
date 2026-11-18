package mx.udg.alumnos.tanken;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.time.LocalTime;

/** Native Android picker; DialogFragment restores its unconfirmed selection on rotation. */
public final class LessonTimePicker extends DialogFragment {
    static final String RESULT = "lessonTime";

    static LessonTimePicker create(boolean start, String value) {
        LocalTime time = LocalTime.parse(value);
        Bundle args = new Bundle();
        args.putBoolean("start", start);
        args.putInt("hour", time.getHour());
        args.putInt("minute", time.getMinute());
        LessonTimePicker fragment = new LessonTimePicker();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle saved) {
        Bundle args = requireArguments();
        TimePickerDialog dialog =
                new TimePickerDialog(
                        requireContext(),
                        (view, hour, minute) -> {
                            Bundle result = new Bundle();
                            result.putBoolean("start", args.getBoolean("start"));
                            result.putInt("hour", hour);
                            result.putInt("minute", minute);
                            getParentFragmentManager().setFragmentResult(RESULT, result);
                        },
                        args.getInt("hour"),
                        args.getInt("minute"),
                        DateFormat.is24HourFormat(requireContext()));
        dialog.setTitle(
                args.getBoolean("start")
                        ? R.string.schedule_start_label
                        : R.string.schedule_end_label);
        return dialog;
    }
}
