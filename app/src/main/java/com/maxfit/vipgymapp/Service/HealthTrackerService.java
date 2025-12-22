package com.maxfit.vipgymapp.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;
import java.util.Locale;

/**
 * HealthTracker Service - Auto-tracks health metrics in background
 * Tracks: Steps, Distance, Calories, Active Minutes
 */
public class HealthTrackerService extends Service implements SensorEventListener {

    private static final String TAG = "HealthTrackerService";
    private static final String PREFS_NAME = "HealthTrackerPrefs";

    // Broadcast action for health data updates
    public static final String ACTION_HEALTH_UPDATE = "com.example.maxfitvipgymapp.HEALTH_UPDATE";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private SharedPreferences prefs;

    // Health data
    private int todaySteps = 0;
    private int initialStepCount = -1;
    private double distanceKm = 0.0;
    private int caloriesBurned = 0;
    private int activeMinutes = 0;

    // User profile data (from SharedPreferences)
    private int userHeightCm = 170; // Default
    private double userWeightKg = 70.0; // Default
    private int userAge = 30; // Default
    private String userGender = "M"; // Default
    private double strideLength = 0.0;

    // Handler for periodic updates
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "HealthTracker Service Created");

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load user profile
        loadUserProfile();

        // Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "✅ Step Counter Sensor registered");
        } else {
            Log.e(TAG, "❌ Step Counter Sensor not available on this device");
        }

        // Load today's data
        loadTodayData();

        // Start periodic updates (every 5 minutes)
        startPeriodicUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "HealthTracker Service Started");
        return START_STICKY; // Restart if killed
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];

            if (initialStepCount == -1) {
                // First reading - initialize
                initialStepCount = totalSteps - getTodayStoredSteps();
                Log.d(TAG, "Initial step count: " + initialStepCount);
            }

            // Calculate today's steps
            todaySteps = totalSteps - initialStepCount;

            // Update derived metrics
            updateDerivedMetrics();

            // Save data
            saveTodayData();

            // Broadcast update
            broadcastHealthUpdate();

            Log.d(TAG, "📊 Steps: " + todaySteps + " | Distance: " + String.format("%.2f", distanceKm) + " km | Calories: " + caloriesBurned);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    /**
     * Calculate distance, calories, and active minutes based on steps
     */
    private void updateDerivedMetrics() {
        // Calculate distance
        if (strideLength == 0) {
            calculateStrideLength();
        }
        distanceKm = (todaySteps * strideLength) / 1000.0; // Convert meters to km

        // Calculate calories burned
        caloriesBurned = calculateCalories();

        // Calculate active minutes (approximate: 100 steps = 1 minute of activity)
        activeMinutes = todaySteps / 100;
    }

    /**
     * Calculate stride length based on height and gender
     */
    private void calculateStrideLength() {
        double heightMeters = userHeightCm / 100.0;

        if (userGender.equals("M")) {
            strideLength = heightMeters * 0.415; // Men's formula
        } else {
            strideLength = heightMeters * 0.413; // Women's formula
        }

        Log.d(TAG, "Calculated stride length: " + strideLength + " meters");
    }

    /**
     * Calculate calories burned
     * Formula: BMR (hourly rate) + Active calories from steps
     */
    private int calculateCalories() {
        // Calculate BMR (Basal Metabolic Rate)
        double bmr;
        if (userGender.equals("M")) {
            // Men: BMR = 88.362 + (13.397 × weight) + (4.799 × height) - (5.677 × age)
            bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
        } else {
            // Women: BMR = 447.593 + (9.247 × weight) + (3.098 × height) - (4.330 × age)
            bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
        }

        // Get hours since midnight
        Calendar cal = Calendar.getInstance();
        double hoursSinceMidnight = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0);

        // Resting calories burned so far today
        double restingCalories = (bmr / 24) * hoursSinceMidnight;

        // Active calories from steps (approximately 0.04 calories per step)
        double activeCalories = todaySteps * 0.04;

        return (int) (restingCalories + activeCalories);
    }

    /**
     * Load user profile from SharedPreferences
     */
    private void loadUserProfile() {
        SharedPreferences userPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        userHeightCm = userPrefs.getInt("height_cm", 170);
        userWeightKg = (double) userPrefs.getFloat("weight_kg", 70.0f);
        userAge = userPrefs.getInt("age", 30);
        userGender = userPrefs.getString("gender", "M");

        Log.d(TAG, "👤 User Profile: H=" + userHeightCm + "cm, W=" + userWeightKg + "kg, Age=" + userAge + ", Gender=" + userGender);
    }

    /**
     * Load today's stored data
     */
    private void loadTodayData() {
        String today = getTodayDate();
        String lastSavedDate = prefs.getString("last_date", "");

        if (today.equals(lastSavedDate)) {
            // Load existing data for today
            todaySteps = prefs.getInt("today_steps", 0);
            initialStepCount = prefs.getInt("initial_step_count", -1);
            Log.d(TAG, "📅 Loaded today's data: " + todaySteps + " steps");
        } else {
            // New day - reset
            todaySteps = 0;
            initialStepCount = -1;
            saveTodayData();
            Log.d(TAG, "📅 New day started - data reset");
        }

        updateDerivedMetrics();
    }

    /**
     * Save today's data
     */
    private void saveTodayData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_date", getTodayDate());
        editor.putInt("today_steps", todaySteps);
        editor.putInt("initial_step_count", initialStepCount);
        editor.apply();
    }

    /**
     * Get today's steps from storage
     */
    private int getTodayStoredSteps() {
        String today = getTodayDate();
        String lastSavedDate = prefs.getString("last_date", "");

        if (today.equals(lastSavedDate)) {
            return prefs.getInt("today_steps", 0);
        }
        return 0;
    }

    /**
     * Get today's date as string
     */
    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Broadcast health data update to UI
     */
    private void broadcastHealthUpdate() {
        Intent intent = new Intent(ACTION_HEALTH_UPDATE);
        intent.putExtra("steps", todaySteps);
        intent.putExtra("distance_km", distanceKm);
        intent.putExtra("calories", caloriesBurned);
        intent.putExtra("active_minutes", activeMinutes);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Start periodic updates every 5 minutes
     */
    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Recalculate metrics (in case time-based calculations need updating)
                updateDerivedMetrics();
                saveTodayData();
                broadcastHealthUpdate();

                // Schedule next update in 5 minutes
                updateHandler.postDelayed(this, 5 * 60 * 1000);
            }
        };

        // Start first update in 5 minutes
        updateHandler.postDelayed(updateRunnable, 5 * 60 * 1000);
    }

    /**
     * Public method to get current steps (called from other components)
     */
    public static int getCurrentSteps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = getTodayDateStatic();
        String lastDate = prefs.getString("last_date", "");

        if (today.equals(lastDate)) {
            return prefs.getInt("today_steps", 0);
        }
        return 0;
    }

    /**
     * Static helper to get today's date
     */
    private static String getTodayDateStatic() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister sensor
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Stop periodic updates
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }

        Log.d(TAG, "HealthTracker Service Destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}