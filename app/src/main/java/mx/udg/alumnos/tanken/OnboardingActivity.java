package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import mx.udg.alumnos.tanken.databinding.ActivityOnboardingBinding;

public final class OnboardingActivity extends AppCompatActivity {
    static final String REPLAY = "replay";
    private ActivityOnboardingBinding b;
    private OnboardingAdapter pages;
    private final NotificationAccess notifications =
            new NotificationAccess(this, () -> this, this::refreshPermission);
    private final ViewPager2.OnPageChangeCallback selection =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int page) {
                    render(page);
                }
            };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        ReminderNotifications.createChannel(this);
        TimerNotifications.createChannel(this);
        pages = new OnboardingAdapter(notifications::requestOrSettings);
        b.pages.setAdapter(pages);
        b.pages.registerOnPageChangeCallback(selection);
        b.pages.setCurrentItem(saved == null ? 0 : saved.getInt("page", 0), false);
        b.previous.setOnClickListener(v -> b.pages.setCurrentItem(b.pages.getCurrentItem() - 1));
        b.next.setOnClickListener(
                v -> {
                    int page = b.pages.getCurrentItem();
                    if (page == OnboardingAdapter.COUNT - 1) complete();
                    else b.pages.setCurrentItem(page + 1);
                });
        b.skip.setText(
                getIntent().getBooleanExtra(REPLAY, false)
                        ? R.string.welcome_close
                        : R.string.welcome_skip);
        b.skip.setOnClickListener(v -> complete());
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (b.pages.getCurrentItem() > 0)
                                    b.pages.setCurrentItem(b.pages.getCurrentItem() - 1);
                                else if (getIntent().getBooleanExtra(REPLAY, false)) finish();
                                else finishAffinity(); // Leaving is not the same as explicitly
                                // skipping the tutorial.
                            }
                        });
        render(b.pages.getCurrentItem());
    }

    private void render(int page) {
        if (b == null) return;
        b.step.setText(getString(R.string.welcome_step, page + 1, OnboardingAdapter.COUNT));
        b.progress.setMax(OnboardingAdapter.COUNT);
        b.progress.setProgressCompat(page + 1, false);
        b.previous.setVisibility(page == 0 ? View.INVISIBLE : View.VISIBLE);
        b.previous.setEnabled(page != 0);
        boolean last = page == OnboardingAdapter.COUNT - 1;
        b.next.setText(last ? R.string.welcome_finish : R.string.welcome_next);
        b.next.setIconResource(last ? R.drawable.ic_ms_done : R.drawable.ic_ms_skip_next);
    }

    private void complete() {
        if (!getIntent().getBooleanExtra(REPLAY, false)) OnboardingState.complete(this);
        finish();
    }

    private void refreshPermission() {
        if (b != null && pages != null) pages.notifyItemChanged(OnboardingAdapter.COUNT - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle saved) {
        saved.putInt("page", b.pages.getCurrentItem());
        super.onSaveInstanceState(saved);
    }

    @Override
    protected void onDestroy() {
        b.pages.unregisterOnPageChangeCallback(selection);
        b.pages.setAdapter(null);
        b = null;
        super.onDestroy();
    }
}
