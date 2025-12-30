package com.maxfit.vipgymapp.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.maxfit.vipgymapp.Activity.MainActivity;
import com.maxfit.vipgymapp.R;
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

    private static final String CHANNEL_ID = "health_tracker_channel";
    private static final int NOTIFICATION_ID = 1002;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private SharedPreferences prefs;
    private ExecutorService databaseExecutor;

    // Health data
    private volatile int todaySteps = 0;
    private volatile int sensorBaselineSteps = -1; // ✅ RENAMED for clarity
    private volatile double distanceKm = 0.0;
    private volatile int caloriesBurned = 0;
    private volatile int activeMinutes = 0;

    // ✅ NEW: Track when user started being active
    private long sessionStartTime = 0;
    private long totalActiveTimeMs = 0;
    private int lastStepCount = 0;
    private long lastStepTime = 0;
    private static final long ACTIVE_THRESHOLD_MS = 60000; // 1 minute of no steps = inactive

    // User profile
    private int userHeightCm = 170;
    private double userWeightKg = 70.0;
    private int userAge = 30;
    private String userGender = "M";
    private double strideLength = 0.0;

    private Handler updateHandler;
    private Runnable updateRunnable;
    private Handler realtimeHandler;
    private Runnable realtimeUpdateRunnable;

    private volatile boolean isServiceRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ HealthTracker Service Created");

        try {
            updateHandler = new Handler(Looper.getMainLooper());
            realtimeHandler = new Handler(Looper.getMainLooper());

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            databaseExecutor = Executors.newSingleThreadExecutor();

            createNotificationChannel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }

            loadUserProfile();

            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

                if (stepCounterSensor != null) {
                    // ✅ FIXED: Register with faster update rate for better accuracy
                    boolean registered = sensorManager.registerListener(
                            this,
                            stepCounterSensor,
                            SensorManager.SENSOR_DELAY_NORMAL
                    );

                    if (registered) {
                        Log.d(TAG, "✅ Step Counter Sensor registered successfully");
                    } else {
                        Log.e(TAG, "❌ Failed to register Step Counter Sensor");
                    }
                } else {
                    Log.e(TAG, "❌ Step Counter Sensor NOT available on this device");
                }
            } else {
                Log.e(TAG, "❌ SensorManager is null");
            }

            checkAndSavePreviousDayData();
            loadTodayData();
            updateDerivedMetrics();
            broadcastHealthUpdate();
            startPeriodicUpdates();
            startRealtimeUpdates();

            Log.d(TAG, "✅ Service fully initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Health Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks your daily activity");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            }
        }
    }

    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            // ✅ Show current steps in notification
            String contentText = String.format(Locale.US,
                    "%,d steps • %.2f km", todaySteps, distanceKm);

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Health Tracking Active")
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.running)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage(), e);
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Health Tracking")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
        }
    }

    // ✅ NEW: Update notification with current stats
    private void updateNotification() {
        try {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage(), e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isServiceRunning) return;

        try {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                int sensorTotalSteps = (int) event.values[0];
                long currentTime = System.currentTimeMillis();

                Log.d(TAG, "📱 Sensor reading: " + sensorTotalSteps + " total steps from device boot");

                String today = getTodayDate();
                String lastSavedDate = prefs.getString("last_date", "");

                // ✅ FIXED: Check if day changed
                if (!today.equals(lastSavedDate)) {
                    Log.d(TAG, "🗓️ Day changed! Saving previous day and resetting...");
                    checkAndSavePreviousDayData();

                    // Reset everything for new day
                    sensorBaselineSteps = sensorTotalSteps;
                    todaySteps = 0;
                    totalActiveTimeMs = 0;
                    sessionStartTime = 0;
                    lastStepCount = 0;

                    saveTodayData();
                    Log.d(TAG, "✅ Reset complete. New baseline: " + sensorBaselineSteps);
                }

                // ✅ FIXED: Initialize baseline on first run
                if (sensorBaselineSteps == -1) {
                    int storedSteps = getTodayStoredSteps();
                    sensorBaselineSteps = sensorTotalSteps - storedSteps;
                    Log.d(TAG, "🔧 Initialized baseline: " + sensorBaselineSteps +
                            " (sensor: " + sensorTotalSteps + ", stored: " + storedSteps + ")");
                }

                // ✅ FIXED: Calculate today's steps
                int newTodaySteps = sensorTotalSteps - sensorBaselineSteps;

                // ✅ Handle sensor resets (rare but possible)
                if (newTodaySteps < 0) {
                    Log.w(TAG, "⚠️ Negative steps detected! Sensor may have reset. Adjusting...");
                    sensorBaselineSteps = sensorTotalSteps;
                    newTodaySteps = 0;
                }

                // ✅ FIXED: Update active time tracking
                if (newTodaySteps > todaySteps) {
                    // User is walking
                    if (sessionStartTime == 0) {
                        sessionStartTime = currentTime;
                        Log.d(TAG, "🚶 Activity session started");
                    }
                    lastStepTime = currentTime;
                    lastStepCount = newTodaySteps;
                } else {
                    // Check if user stopped walking
                    if (sessionStartTime > 0 &&
                            (currentTime - lastStepTime) > ACTIVE_THRESHOLD_MS) {
                        // Add this session to total active time
                        long sessionDuration = lastStepTime - sessionStartTime;
                        totalActiveTimeMs += sessionDuration;
                        sessionStartTime = 0;
                        Log.d(TAG, "⏸️ Activity session ended. Duration: " +
                                (sessionDuration / 1000) + "s");
                    }
                }

                todaySteps = newTodaySteps;

                Log.d(TAG, "📊 Today's steps: " + todaySteps +
                        " (baseline: " + sensorBaselineSteps +
                        ", sensor total: " + sensorTotalSteps + ")");

                updateDerivedMetrics();
                saveTodayData();
                broadcastHealthUpdate();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onSensorChanged: " + e.getMessage(), e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }

    private void updateDerivedMetrics() {
        try {
            if (strideLength == 0) calculateStrideLength();

            // Distance
            distanceKm = (todaySteps * strideLength) / 1000.0;

            // Calories
            caloriesBurned = calculateCalories();

            // ✅ FIXED: Active time calculation
            long currentActiveTime = totalActiveTimeMs;
            if (sessionStartTime > 0) {
                // Add current ongoing session
                currentActiveTime += (System.currentTimeMillis() - sessionStartTime);
            }
            activeMinutes = (int) (currentActiveTime / 60000); // Convert ms to minutes

            Log.d(TAG, "📊 Metrics - Steps: " + todaySteps +
                    ", Distance: " + String.format("%.2f", distanceKm) + " km" +
                    ", Calories: " + caloriesBurned +
                    ", Active: " + activeMinutes + " min");

        } catch (Exception e) {
            Log.e(TAG, "Error updating metrics: " + e.getMessage(), e);
        }
    }

    private void calculateStrideLength() {
        double heightMeters = userHeightCm / 100.0;
        strideLength = (userGender.equals("M")) ? heightMeters * 0.415 : heightMeters * 0.413;
        Log.d(TAG, "Calculated stride length: " + strideLength + "m");
    }

    private int calculateCalories() {
        try {
            double bmr;
            if (userGender.equals("M")) {
                bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
            } else {
                bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
            }

            Calendar cal = Calendar.getInstance();
            double hoursSinceMidnight = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0);
            double restingCalories = (bmr / 24) * hoursSinceMidnight;

            // ✅ More accurate activity calories (0.04 per step is standard)
            double activityCalories = todaySteps * 0.04;

            return (int) (restingCalories + activityCalories);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating calories: " + e.getMessage(), e);
            return 0;
        }
    }

    private void loadUserProfile() {
        try {
            SharedPreferences userPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            userHeightCm = userPrefs.getInt("height_cm", 170);
            userWeightKg = (double) userPrefs.getFloat("weight_kg", 70.0f);
            userAge = userPrefs.getInt("age", 30);
            userGender = userPrefs.getString("gender", "M");

            Log.d(TAG, "User profile loaded: " + userHeightCm + "cm, " +
                    userWeightKg + "kg, " + userAge + "y, " + userGender);
        } catch (Exception e) {
            Log.e(TAG, "Error loading user profile: " + e.getMessage(), e);
        }
    }

    private void loadTodayData() {
        try {
            String today = getTodayDate();
            String lastSavedDate = prefs.getString("last_date", "");

            if (today.equals(lastSavedDate)) {
                todaySteps = prefs.getInt("today_steps", 0);
                sensorBaselineSteps = prefs.getInt("sensor_baseline", -1);
                totalActiveTimeMs = prefs.getLong("total_active_time_ms", 0);

                Log.d(TAG, "📱 Loaded today's data: " + todaySteps + " steps, " +
                        "baseline: " + sensorBaselineSteps +
                        ", active time: " + (totalActiveTimeMs / 60000) + " min");
            } else {
                Log.d(TAG, "📱 No data for today, starting fresh");
                todaySteps = 0;
                sensorBaselineSteps = -1;
                totalActiveTimeMs = 0;
                saveTodayData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading today's data: " + e.getMessage(), e);
        }
    }

    private void saveTodayData() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_date", getTodayDate());
            editor.putInt("today_steps", todaySteps);
            editor.putInt("sensor_baseline", sensorBaselineSteps);
            editor.putLong("total_active_time_ms", totalActiveTimeMs);
            editor.apply();

            Log.d(TAG, "💾 Saved: " + todaySteps + " steps, baseline: " + sensorBaselineSteps);
        } catch (Exception e) {
            Log.e(TAG, "Error saving data: " + e.getMessage(), e);
        }
    }

    private int getTodayStoredSteps() {
        try {
            String today = getTodayDate();
            if (today.equals(prefs.getString("last_date", ""))) {
                return prefs.getInt("today_steps", 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting stored steps: " + e.getMessage(), e);
        }
        return 0;
    }

    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private void broadcastHealthUpdate() {
        if (!isServiceRunning) return;

        try {
            Intent intent = new Intent(ACTION_HEALTH_UPDATE);
            intent.putExtra("steps", todaySteps);
            intent.putExtra("distance_km", distanceKm);
            intent.putExtra("calories", caloriesBurned);
            intent.putExtra("active_minutes", activeMinutes);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            Log.d(TAG, "📡 Broadcast: " + todaySteps + " steps, " +
                    activeMinutes + " active min");

            // ✅ Update notification too
            updateNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting: " + e.getMessage(), e);
        }
    }

    private void checkAndSavePreviousDayData() {
        try {
            String today = getTodayDate();
            String lastSavedDate = prefs.getString("last_date", "");

            if (!lastSavedDate.isEmpty() && !today.equals(lastSavedDate)) {
                Log.d(TAG, "🗓️ Day changed from " + lastSavedDate + " to " + today);

                int savedSteps = prefs.getInt("today_steps", 0);
                long savedActiveTimeMs = prefs.getLong("total_active_time_ms", 0);
                int savedActiveMinutes = (int) (savedActiveTimeMs / 60000);

                calculateStrideLength();
                double savedDist = (savedSteps * strideLength) / 1000.0;

                double bmr;
                if (userGender.equals("M")) {
                    bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
                } else {
                    bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
                }
                int savedCalories = (int) (bmr + (savedSteps * 0.04));

                saveToDatabase(lastSavedDate, savedSteps, savedDist, savedCalories, savedActiveMinutes);

                todaySteps = 0;
                sensorBaselineSteps = -1;
                totalActiveTimeMs = 0;
                saveTodayData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking/saving previous day: " + e.getMessage(), e);
        }
    }

    private void saveToDatabase(String date, int steps, double dist, int cal, int active) {
        try {
            SessionManager sessionManager = new SessionManager(this);
            int memberId = sessionManager.getMemberId();

            if (memberId != -1) {
                databaseExecutor.execute(() -> {
                    try {
                        HealthRepository repo = new HealthRepository();
                        repo.saveDailyStats(memberId, date, steps, dist, cal, active);
                        Log.d(TAG, "✅ Saved to DB: " + date + " - " + steps + " steps");
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving to DB: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in saveToDatabase: " + e.getMessage(), e);
        }
    }

    private void startPeriodicUpdates() {
        if (!isServiceRunning || updateHandler == null) return;

        try {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isServiceRunning) return;
                    try {
                        checkAndSavePreviousDayData();
                        updateDerivedMetrics();
                        saveTodayData();
                        if (updateHandler != null) {
                            updateHandler.postDelayed(this, 5 * 60 * 1000); // Every 5 min
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in periodic update: " + e.getMessage(), e);
                    }
                }
            };
            updateHandler.postDelayed(updateRunnable, 5 * 60 * 1000);
            Log.d(TAG, "✅ Periodic updates started (every 5 min)");
        } catch (Exception e) {
            Log.e(TAG, "Error starting periodic updates: " + e.getMessage(), e);
        }
    }

    private void startRealtimeUpdates() {
        if (!isServiceRunning || realtimeHandler == null) return;

        try {
            realtimeUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isServiceRunning) return;
                    try {
                        updateDerivedMetrics();
                        broadcastHealthUpdate();
                        if (realtimeHandler != null) {
                            realtimeHandler.postDelayed(this, 10 * 1000); // Every 10 seconds
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in realtime update: " + e.getMessage(), e);
                    }
                }
            };
            realtimeHandler.post(realtimeUpdateRunnable);
            Log.d(TAG, "✅ Realtime updates started (every 10 sec)");
        } catch (Exception e) {
            Log.e(TAG, "Error starting realtime updates: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;

        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            if (updateHandler != null && updateRunnable != null) {
                updateHandler.removeCallbacks(updateRunnable);
            }
            if (realtimeHandler != null && realtimeUpdateRunnable != null) {
                realtimeHandler.removeCallbacks(realtimeUpdateRunnable);
            }
            if (databaseExecutor != null) {
                databaseExecutor.shutdown();
            }
            Log.d(TAG, "🛑 HealthTracker Service Destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}