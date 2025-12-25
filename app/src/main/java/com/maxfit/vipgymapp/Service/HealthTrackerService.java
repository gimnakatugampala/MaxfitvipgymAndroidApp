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

import com.maxfit.vipgymapp.Repository.HealthRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthTrackerService extends Service implements SensorEventListener {

    private static final String TAG = "HealthTrackerService";
    private static final String PREFS_NAME = "HealthTrackerPrefs";
    public static final String ACTION_HEALTH_UPDATE = "com.maxfit.vipgymapp.HEALTH_UPDATE";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private SharedPreferences prefs;
    private ExecutorService databaseExecutor; // For background DB operations

    // Health data
    private int todaySteps = 0;
    private int initialStepCount = -1;
    private double distanceKm = 0.0;
    private int caloriesBurned = 0;
    private int activeMinutes = 0;

    // User profile
    private int userHeightCm = 170;
    private double userWeightKg = 70.0;
    private int userAge = 30;
    private String userGender = "M";
    private double strideLength = 0.0;

    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;

    private Handler realtimeHandler = new Handler();
    private Runnable realtimeUpdateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ HealthTracker Service Created");

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseExecutor = Executors.newSingleThreadExecutor(); // Init executor

        loadUserProfile();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.e(TAG, "❌ Step Counter Sensor NOT available");
        }

        // ✅ Check if day changed while app was closed
        checkAndSavePreviousDayData();

        loadTodayData();
        updateDerivedMetrics();
        broadcastHealthUpdate();
        startPeriodicUpdates();
        startRealtimeUpdates();
    }

    // ✅ NEW METHOD: Check if the date has changed and save old data
    private void checkAndSavePreviousDayData() {
        String today = getTodayDate();
        String lastSavedDate = prefs.getString("last_date", "");

        // If stored date is different from today, and not empty, it means we have data from a past day
        if (!lastSavedDate.isEmpty() && !today.equals(lastSavedDate)) {
            Log.d(TAG, "🗓 Day changed from " + lastSavedDate + " to " + today + ". Saving yesterday's data.");

            int savedSteps = prefs.getInt("today_steps", 0);

            // Re-calculate metrics for the saved steps to ensure accuracy
            calculateStrideLength();
            double savedDist = (savedSteps * strideLength) / 1000.0;

            // Calculate BMR based calories for a full 24-hour day
            double bmr;
            if (userGender.equals("M")) {
                bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
            } else {
                bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
            }
            int savedCalories = (int) (bmr + (savedSteps * 0.04)); // Full BMR + Active Calories
            int savedActive = savedSteps / 100;

            // Upload to Database
            saveToDatabase(lastSavedDate, savedSteps, savedDist, savedCalories, savedActive);

            // Reset for today
            todaySteps = 0;
            initialStepCount = -1; // Will be reset on next sensor event
            saveTodayData(); // Save the reset state with today's date
        }
    }

    // ✅ NEW METHOD: Upload to Database
    private void saveToDatabase(String date, int steps, double dist, int cal, int active) {
        SessionManager sessionManager = new SessionManager(this);
        int memberId = sessionManager.getMemberId();

        if (memberId != -1) {
            databaseExecutor.execute(() -> {
                HealthRepository repo = new HealthRepository();
                repo.saveDailyStats(memberId, date, steps, dist, cal, active);
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];

            // ✅ Check for Day Change while app is running
            String today = getTodayDate();
            String lastSavedDate = prefs.getString("last_date", "");

            if (!today.equals(lastSavedDate)) {
                // Day changed while running! Save current `todaySteps` as yesterday's data
                checkAndSavePreviousDayData();

                // Reset baseline for the new day
                initialStepCount = totalSteps;
                todaySteps = 0;
            }

            if (initialStepCount == -1) {
                initialStepCount = totalSteps - getTodayStoredSteps();
                saveTodayData();
            }

            int newTodaySteps = totalSteps - initialStepCount;
            if (newTodaySteps < 0) {
                initialStepCount = totalSteps;
                newTodaySteps = 0;
            }

            todaySteps = newTodaySteps;
            updateDerivedMetrics();
            saveTodayData();
            broadcastHealthUpdate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateDerivedMetrics() {
        if (strideLength == 0) calculateStrideLength();
        distanceKm = (todaySteps * strideLength) / 1000.0;
        caloriesBurned = calculateCalories();
        activeMinutes = todaySteps / 100;
    }

    private void calculateStrideLength() {
        double heightMeters = userHeightCm / 100.0;
        strideLength = (userGender.equals("M")) ? heightMeters * 0.415 : heightMeters * 0.413;
    }

    private int calculateCalories() {
        double bmr;
        if (userGender.equals("M")) {
            bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
        } else {
            bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
        }
        Calendar cal = Calendar.getInstance();
        double hoursSinceMidnight = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0);
        double restingCalories = (bmr / 24) * hoursSinceMidnight;
        return (int) (restingCalories + (todaySteps * 0.04));
    }

    private void loadUserProfile() {
        SharedPreferences userPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        userHeightCm = userPrefs.getInt("height_cm", 170);
        userWeightKg = (double) userPrefs.getFloat("weight_kg", 70.0f);
        userAge = userPrefs.getInt("age", 30);
        userGender = userPrefs.getString("gender", "M");
    }

    private void loadTodayData() {
        String today = getTodayDate();
        String lastSavedDate = prefs.getString("last_date", "");
        if (today.equals(lastSavedDate)) {
            todaySteps = prefs.getInt("today_steps", 0);
            initialStepCount = prefs.getInt("initial_step_count", -1);
        } else {
            todaySteps = 0;
            initialStepCount = -1;
            saveTodayData();
        }
    }

    private void saveTodayData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_date", getTodayDate());
        editor.putInt("today_steps", todaySteps);
        editor.putInt("initial_step_count", initialStepCount);
        editor.apply();
    }

    private int getTodayStoredSteps() {
        String today = getTodayDate();
        if (today.equals(prefs.getString("last_date", ""))) {
            return prefs.getInt("today_steps", 0);
        }
        return 0;
    }

    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private void broadcastHealthUpdate() {
        Intent intent = new Intent(ACTION_HEALTH_UPDATE);
        intent.putExtra("steps", todaySteps);
        intent.putExtra("distance_km", distanceKm);
        intent.putExtra("calories", caloriesBurned);
        intent.putExtra("active_minutes", activeMinutes);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Check date again in case it changed during quiet period
                checkAndSavePreviousDayData();

                updateDerivedMetrics();
                saveTodayData();
                updateHandler.postDelayed(this, 5 * 60 * 1000);
            }
        };
        updateHandler.postDelayed(updateRunnable, 5 * 60 * 1000);
    }

    private void startRealtimeUpdates() {
        realtimeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDerivedMetrics();
                broadcastHealthUpdate();
                realtimeHandler.postDelayed(this, 30 * 1000);
            }
        };
        realtimeHandler.post(realtimeUpdateRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (updateHandler != null) updateHandler.removeCallbacks(updateRunnable);
        if (realtimeHandler != null) realtimeHandler.removeCallbacks(realtimeUpdateRunnable);
        if (databaseExecutor != null) databaseExecutor.shutdown();
        Log.d(TAG, "🛑 HealthTracker Service Destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}