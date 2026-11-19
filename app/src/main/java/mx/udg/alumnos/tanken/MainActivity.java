package mx.udg.alumnos.tanken;

import android.content.*;
import android.os.Bundle;
import android.view.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.*;

import mx.udg.alumnos.tanken.databinding.ActivityMainBinding;

public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    private NavController navigation;
    private AppBarConfiguration appBar;
    private boolean welcomeLaunched;
    private boolean notificationExplained;
    private final NotificationAccess notifications =
            new NotificationAccess(this, () -> this, () -> {});

    public static Intent destination(Context context, String tab) {
        return new Intent(context, MainActivity.class)
                .putExtra("destination", tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        NavHostFragment host =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        navigation = host.getNavController();
        appBar =
                new AppBarConfiguration.Builder(
                                R.id.home, R.id.reminders, R.id.history, R.id.services)
                        .build();
        NavigationUI.setupActionBarWithNavController(this, navigation, appBar);
        NavigationUI.setupWithNavController(b.bottomNavigation, navigation);
        ViewCompat.setOnApplyWindowInsetsListener(
                b.getRoot(),
                (view, insets) -> {
                    androidx.core.graphics.Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    androidx.core.graphics.Insets keyboard =
                            insets.getInsets(WindowInsetsCompat.Type.ime());
                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            Math.max(bars.bottom, keyboard.bottom));
                    b.bottomNavigation.setVisibility(
                            insets.isVisible(WindowInsetsCompat.Type.ime())
                                    ? View.GONE
                                    : View.VISIBLE);
                    return insets;
                });
        if (saved == null) route(getIntent());
        welcomeLaunched = saved != null && saved.getBoolean("welcomeLaunched", false);
        notificationExplained = saved != null && saved.getBoolean("notificationExplained", false);
        if (!OnboardingState.completed(this) && !welcomeLaunched) {
            welcomeLaunched = true;
            startActivity(new Intent(this, OnboardingActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OnboardingState.completed(this)
                && !notificationExplained
                && !NotificationAccess.granted(this)) {
            notificationExplained = true;
            notifications.explainBlocked(ReminderNotifications.CHANNEL);
        }
    }

    public void select(int destination) {
        b.bottomNavigation.setSelectedItemId(destination);
    }

    private void route(Intent intent) {
        String target = intent.getStringExtra("destination");
        if ("reminders".equals(target)) select(R.id.reminders);
        else if ("history".equals(target)) select(R.id.history);
        else if ("services".equals(target)) select(R.id.services);
        String id = intent.getStringExtra("noticeId");
        if (id != null) {
            select(R.id.reminders);
            Bundle args = new Bundle();
            args.putString("id", id);
            NavHostFragment host =
                    (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
            host.getChildFragmentManager().setFragmentResult("openNotice", args);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        route(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Class<?> screen =
                item.getItemId() == R.id.openSettings
                        ? SettingsActivity.class
                        : item.getItemId() == R.id.openHelp
                                ? HelpActivity.class
                                : item.getItemId() == R.id.openAbout ? AboutActivity.class : null;
        if (screen != null) {
            startActivity(new Intent(this, screen));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navigation, appBar) || super.onSupportNavigateUp();
    }

    @Override
    protected void onSaveInstanceState(Bundle saved) {
        saved.putBoolean("welcomeLaunched", welcomeLaunched);
        saved.putBoolean("notificationExplained", notificationExplained);
        super.onSaveInstanceState(saved);
    }

    @Override
    protected void onDestroy() {
        notifications.dismiss();
        b = null;
        super.onDestroy();
    }
}
