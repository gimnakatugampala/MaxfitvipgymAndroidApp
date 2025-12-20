package com.example.maxfitvipgymapp.Activity;

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

import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Fragments.HomeFragment;
import com.example.maxfitvipgymapp.Fragments.InsightsFragment;
import com.example.maxfitvipgymapp.Fragments.ProfileFragment;
import com.example.maxfitvipgymapp.Widget.WorkoutWidgetProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private HashMap<Integer, Fragment> fragmentMap;
    private static final String PREFS_NAME = "WidgetPrefs";
    private static final String KEY_WIDGET_ASKED = "widget_asked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_insights, new InsightsFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        loadFragment(fragmentMap.get(R.id.nav_home));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            return selectedFragment != null && loadFragment(selectedFragment);
        });

        // Ask to pin widget on first launch
        askToPinWidget();
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
            // Check if widget is already added
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName componentName = new ComponentName(this, WorkoutWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length == 0) {
                // Widget not added, show dialog
                new AlertDialog.Builder(this)
                        .setTitle("Add Home Screen Widget")
                        .setMessage("Add a widget to your home screen to track your workout streak and get quick access to start workouts!")
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
                // Widget already added
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
                        this,
                        0,
                        pinnedWidgetCallbackIntent,
                        PendingIntent.FLAG_IMMUTABLE
                );

                boolean success = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback);

                if (success) {
                    Toast.makeText(this, "Please place the widget on your home screen", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Unable to add widget. Please add manually from widgets menu", Toast.LENGTH_LONG).show();
                }
            } else {
                // Fallback for devices that don't support pinning
                Toast.makeText(this, "Long press home screen → Widgets → MaxfitvipgymApp", Toast.LENGTH_LONG).show();
            }
        }
    }
}