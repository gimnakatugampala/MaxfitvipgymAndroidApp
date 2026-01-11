package com.maxfit.vipgymapp.Activity;

import com.maxfit.vipgymapp.Worker.DailyMotivationWorker;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Fragments.HomeFragment;
import com.maxfit.vipgymapp.Fragments.InsightsFragment;
import com.maxfit.vipgymapp.Fragments.ProfileFragment;
import com.maxfit.vipgymapp.Repository.MemberRepository;
import com.maxfit.vipgymapp.Service.HealthTrackerService;
import com.maxfit.vipgymapp.Utils.SessionManager;
import com.maxfit.vipgymapp.Widget.WorkoutWidgetProvider;
import com.maxfit.vipgymapp.Worker.DailySyncWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private HashMap<Integer, Fragment> fragmentMap;
    private static final String PREFS_NAME = "WidgetPrefs";
    private static final String KEY_WIDGET_ASKED = "widget_asked";
    private SessionManager sessionManager;
    private MemberRepository memberRepository;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Initialize repositories and executors
        sessionManager = new SessionManager(this);
        memberRepository = new MemberRepository();
        executorService = Executors.newSingleThreadExecutor();

        // ✅ Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // ✅ Check if user is approved (is_active = true)
        checkUserApprovalStatus();

        // ✅ NEW: Update Last Active Timestamp
        updateMemberLastActive();

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
            }
        }

        Intent serviceIntent = new Intent(this, HealthTrackerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_insights, new InsightsFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        // Handle navigation from widget or notification
        String navigateTo = getIntent().getStringExtra("navigateTo");
        if ("home".equals(navigateTo)) {
            loadFragment(fragmentMap.get(R.id.nav_home));
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            loadFragment(fragmentMap.get(R.id.nav_home));
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            return selectedFragment != null && loadFragment(selectedFragment);
        });

        // Ask to pin widget on first launch
        askToPinWidget();

        // ✅ SCHEDULE BACKGROUND SYNC
        scheduleDailySync();

        // ✅ SCHEDULE DAILY MOTIVATION
        scheduleMotivationWorker();
    }

    // ✅ NEW METHOD: Updates the "last_active" timestamp in the database
    private void updateMemberLastActive() {
        if (sessionManager.isLoggedIn()) {
            int memberId = sessionManager.getMemberId();
            executorService.execute(() -> {
                // This calls the method we added to MemberRepository previously
                memberRepository.updateLastActive(memberId);
            });
        }
    }

    // Schedule Motivation Notification
    private void scheduleMotivationWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest motivationRequest =
                new PeriodicWorkRequest.Builder(DailyMotivationWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyMotivation",
                ExistingPeriodicWorkPolicy.KEEP,
                motivationRequest
        );
    }

    // Check if user account is approved
    private void checkUserApprovalStatus() {
        Member member = sessionManager.getMemberData();
        if (member == null) {
            redirectToLogin();
            return;
        }

        executorService.execute(() -> {
            try {
                Member freshMember = memberRepository.getMemberById(member.getId());

                if (freshMember == null) {
                    runOnUiThread(this::redirectToLogin);
                    return;
                }

                if (!freshMember.isActive()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Your account is pending approval", Toast.LENGTH_LONG).show();
                        redirectToWaitingScreen(freshMember);
                    });
                }

                runOnUiThread(() -> sessionManager.updateMemberData(freshMember));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Schedule Background Sync
    private void scheduleDailySync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(DailySyncWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyHealthSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, GetStartedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToWaitingScreen(Member member) {
        Intent intent = new Intent(this, WaitingApprovalActivity.class);
        intent.putExtra("firstName", member.getFirstName());
        intent.putExtra("phoneNumber", member.getPhoneNumber());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void askToPinWidget() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasAsked = prefs.getBoolean(KEY_WIDGET_ASKED, false);

        if (!hasAsked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName componentName = new ComponentName(this, WorkoutWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Add Home Screen Widget")
                        .setMessage("Add a widget to your home screen to track your workout streak!")
                        .setPositiveButton("Add Widget", (dialog, which) -> {
                            pinWidget();
                            prefs.edit().putBoolean(KEY_WIDGET_ASKED, true).apply();
                        })
                        .setNegativeButton("Later", (dialog, which) -> {
                            prefs.edit().putBoolean(KEY_WIDGET_ASKED, true).apply();
                        })
                        .setCancelable(true)
                        .show();
            } else {
                prefs.edit().putBoolean(KEY_WIDGET_ASKED, true).apply();
            }
        }
    }

    private void pinWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
            ComponentName myProvider = new ComponentName(this, WorkoutWidgetProvider.class);

            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
                Intent pinnedWidgetCallbackIntent = new Intent(this, MainActivity.class);
                PendingIntent successCallback = PendingIntent.getActivity(
                        this, 0, pinnedWidgetCallbackIntent, PendingIntent.FLAG_IMMUTABLE
                );

                boolean success = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback);

                if (success) {
                    Toast.makeText(this, "Please place the widget on your home screen",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Unable to add widget", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Long press home screen → Widgets → MaxFit",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        } else {
            checkUserApprovalStatus();
            // Optional: Update last active on resume as well
            updateMemberLastActive();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}