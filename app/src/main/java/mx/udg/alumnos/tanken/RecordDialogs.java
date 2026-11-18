package mx.udg.alumnos.tanken;

import android.content.Intent;

import androidx.appcompat.app.*;
import androidx.lifecycle.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class RecordDialogs {
    private RecordDialogs() {}

    static void observe(androidx.fragment.app.Fragment fragment, RecordsViewModel model) {
        android.app.Activity activity = fragment.requireActivity();
        AlertDialog[] active = {null};
        model.detail()
                .observe(
                        fragment.getViewLifecycleOwner(),
                        detail -> {
                            if (detail == null) return;
                            MaterialAlertDialogBuilder builder =
                                    new MaterialAlertDialogBuilder(activity)
                                            .setTitle(detail.title)
                                            .setMessage(detail.body);
                            if (detail.notice != null && detail.notice.state.equals("ISSUED")) {
                                builder.setPositiveButton(
                                                R.string.notice_done,
                                                (d, w) ->
                                                        model.respond(
                                                                detail.notice,
                                                                NoticeRules.State.COMPLETED))
                                        .setNeutralButton(
                                                R.string.notice_snooze,
                                                (d, w) ->
                                                        model.respond(
                                                                detail.notice,
                                                                NoticeRules.State.SNOOZED))
                                        .setNegativeButton(
                                                R.string.notice_skip,
                                                (d, w) ->
                                                        model.respond(
                                                                detail.notice,
                                                                NoticeRules.State.SKIPPED));
                            } else if (detail.comparisonId != null) {
                                builder.setPositiveButton(
                                                R.string.comparison_replay,
                                                (d, w) ->
                                                        activity.startActivity(
                                                                new Intent(
                                                                                activity,
                                                                                WellnessActivity
                                                                                        .class)
                                                                        .putExtra(
                                                                                "comparisonId",
                                                                                detail.comparisonId)))
                                        .setNegativeButton(R.string.record_close, null);
                            } else builder.setPositiveButton(R.string.record_close, null);
                            active[0] = builder.create();
                            active[0].setOnDismissListener(
                                    d -> {
                                        active[0] = null;
                                        if (!activity.isChangingConfigurations())
                                            model.dismissDetail();
                                    });
                            active[0].show();
                        });
        fragment.getViewLifecycleOwner()
                .getLifecycle()
                .addObserver(
                        new DefaultLifecycleObserver() {
                            @Override
                            public void onDestroy(LifecycleOwner owner) {
                                if (active[0] != null) active[0].dismiss();
                            }
                        });
    }
}
