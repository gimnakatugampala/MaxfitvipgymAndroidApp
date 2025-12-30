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

    // ✅ Foreground Service Configuration
    private static final String CHANNEL_ID = "health_tracker_channel";
    private static final int NOTIFICATION_ID = 1002;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private SharedPreferences prefs;
    private ExecutorService databaseExecutor;

    // Health data (volatile for thread-safety)
    private volatile int todaySteps = 0;
    private volatile int initialStepCount = -1;
    private volatile double distanceKm = 0.0;
    private volatile int caloriesBurned = 0;
    private volatile int activeMinutes = 0;

    // User profile
    private int userHeightCm = 170;
    private double userWeightKg = 70.0;
    private int userAge = 30;
    private String userGender = "M";
    private double strideLength = 0.0;

    // ✅ Handlers initialized with Looper
    private Handler updateHandler;
    private Runnable updateRunnable;
    private Handler realtimeHandler;
    private Runnable realtimeUpdateRunnable;

    // ✅ Service running flag
    private volatile boolean isServiceRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ HealthTracker Service Created");

        try {
            // ✅ Initialize handlers with main looper
            updateHandler = new Handler(Looper.getMainLooper());
            realtimeHandler = new Handler(Looper.getMainLooper());

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            databaseExecutor = Executors.newSingleThreadExecutor();

            // ✅ Create notification channel and start foreground
            createNotificationChannel();

            // For Android 14+ (API 34+), specify foreground service type
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
                    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
                    Log.d(TAG, "✅ Step Counter Sensor registered");
                } else {
                    Log.e(TAG, "❌ Step Counter Sensor NOT available");
                }
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

    // ✅ Create notification channel for Android O+
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

    // ✅ Create foreground notification
    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Health Tracking Active")
                    .setContentText("Tracking your daily activity")
                    .setSmallIcon(R.drawable.running)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage(), e);
            // Return basic notification as fallback
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Health Tracking")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
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
                int totalSteps = (int) event.values[0];

                String today = getTodayDate();
                String lastSavedDate = prefs.getString("last_date", "");

                if (!today.equals(lastSavedDate)) {
                    checkAndSavePreviousDayData();
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

                Log.d(TAG, "📱 Steps updated: " + todaySteps);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged: " + e.getMessage(), e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    private void updateDerivedMetrics() {
        try {
            if (strideLength == 0) calculateStrideLength();
            distanceKm = (todaySteps * strideLength) / 1000.0;
            caloriesBurned = calculateCalories();
            activeMinutes = todaySteps / 100;
        } catch (Exception e) {
            Log.e(TAG, "Error updating metrics: " + e.getMessage(), e);
        }
    }

    private void calculateStrideLength() {
        double heightMeters = userHeightCm / 100.0;
        strideLength = (userGender.equals("M")) ? heightMeters * 0.415 : heightMeters * 0.413;
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
            return (int) (restingCalories + (todaySteps * 0.04));
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
                initialStepCount = prefs.getInt("initial_step_count", -1);
                Log.d(TAG, "📱 Loaded today's steps: " + todaySteps);
            } else {
                todaySteps = 0;
                initialStepCount = -1;
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
            editor.putInt("initial_step_count", initialStepCount);
            editor.apply();
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
            Log.d(TAG, "📡 Broadcast sent: " + todaySteps + " steps");
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting: " + e.getMessage(), e);
        }
    }

    private void checkAndSavePreviousDayData() {
        try {
            String today = getTodayDate();
            String lastSavedDate = prefs.getString("last_date", "");

            if (!lastSavedDate.isEmpty() && !today.equals(lastSavedDate)) {
                Log.d(TAG, "🗓 Day changed from " + lastSavedDate + " to " + today);

                int savedSteps = prefs.getInt("today_steps", 0);
                calculateStrideLength();
                double savedDist = (savedSteps * strideLength) / 1000.0;

                double bmr;
                if (userGender.equals("M")) {
                    bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
                } else {
                    bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
                }
                int savedCalories = (int) (bmr + (savedSteps * 0.04));
                int savedActive = savedSteps / 100;

                saveToDatabase(lastSavedDate, savedSteps, savedDist, savedCalories, savedActive);

                todaySteps = 0;
                initialStepCount = -1;
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
                        Log.d(TAG, "✅ Saved to DB: " + date);
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
                            updateHandler.postDelayed(this, 5 * 60 * 1000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in periodic update: " + e.getMessage(), e);
                    }
                }
            };
            updateHandler.postDelayed(updateRunnable, 5 * 60 * 1000);
            Log.d(TAG, "✅ Periodic updates started");
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
                            realtimeHandler.postDelayed(this, 30 * 1000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in realtime update: " + e.getMessage(), e);
                    }
                }
            };
            realtimeHandler.post(realtimeUpdateRunnable);
            Log.d(TAG, "✅ Realtime updates started");
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