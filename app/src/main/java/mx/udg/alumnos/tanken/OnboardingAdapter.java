package mx.udg.alumnos.tanken;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import mx.udg.alumnos.tanken.databinding.ItemOnboardingBinding;

/** Six fixed pages; only the notification page has an optional system action. */
final class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.Holder> {
    static final int COUNT = 6;
    private static final int[] TITLES = {
        R.string.welcome_title,
        R.string.welcome_day_title,
        R.string.welcome_notices_title,
        R.string.welcome_motion_title,
        R.string.welcome_data_title,
        R.string.welcome_permission_title
    };
    private static final int[] BODIES = {
        R.string.welcome_body,
        R.string.welcome_day_body,
        R.string.welcome_notices_body,
        R.string.welcome_motion_body,
        R.string.welcome_data_body,
        R.string.welcome_permission_body
    };
    private static final int[] ICONS = {
        R.drawable.tanken_logo,
        R.drawable.ic_ms_calendar_month,
        R.drawable.ic_nav_reminders,
        R.drawable.ic_ms_sensors,
        R.drawable.ic_nav_services,
        R.drawable.ic_nav_reminders
    };
    private final Runnable permission;

    OnboardingAdapter(Runnable permission) {
        this.permission = permission;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemOnboardingBinding b;

        Holder(ItemOnboardingBinding binding) {
            super(binding.getRoot());
            b = binding;
            androidx.core.view.ViewCompat.setAccessibilityHeading(b.title, true);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
                ItemOnboardingBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ItemOnboardingBinding b = holder.b;
        Context context = b.getRoot().getContext();
        b.illustration.setImageResource(ICONS[position]);
        androidx.core.widget.ImageViewCompat.setImageTintList(
                b.illustration,
                position == 0
                        ? null
                        : android.content.res.ColorStateList.valueOf(
                                com.google.android.material.color.MaterialColors.getColor(
                                        b.illustration, androidx.appcompat.R.attr.colorPrimary)));
        b.title.setText(TITLES[position]);
        b.body.setText(BODIES[position]);
        boolean last = position == COUNT - 1;
        b.permissionStatus.setVisibility(last ? View.VISIBLE : View.GONE);
        b.permission.setVisibility(last ? View.VISIBLE : View.GONE);
        if (last) {
            boolean enabled =
                    NotificationAccess.enabled(context, ReminderNotifications.CHANNEL)
                            && NotificationAccess.enabled(context, "class_timers");
            b.permissionStatus.setText(
                    enabled
                            ? R.string.welcome_permission_enabled
                            : R.string.welcome_permission_optional);
            b.permission.setVisibility(enabled ? View.GONE : View.VISIBLE);
            b.permission.setOnClickListener(v -> permission.run());
        } else b.permission.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return COUNT;
    }
}
