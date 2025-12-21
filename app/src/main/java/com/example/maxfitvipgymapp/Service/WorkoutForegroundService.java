package com.example.maxfitvipgymapp.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.R;

public class WorkoutForegroundService extends android.app.Service {

    private static final String TAG = "WorkoutForegroundService";
    public static final String CHANNEL_ID = "WorkoutNotificationChannel";

    // Intent extras
    public static final String EXTRA_WORKOUT_TITLE = "WORKOUT_TITLE";
    public static final String EXTRA_DURATION = "WORKOUT_DURATION";
    public static final String EXTRA_IS_RESTING = "IS_RESTING";
    public static final String EXTRA_TOTAL_WORKOUTS = "TOTAL_WORKOUTS";
    public static final String EXTRA_CURRENT_WORKOUT_INDEX = "CURRENT_WORKOUT_INDEX";

    // Broadcast actions
    public static final String ACTION_WORKOUT_COMPLETE = "com.example.maxfitvipgymapp.WORKOUT_COMPLETE";
    public static final String ACTION_TIMER_TICK = "com.example.maxfitvipgymapp.TIMER_TICK";

    private Handler handler = new Handler();
    private int timeLeft;
    private int totalDuration;
    private String workoutTitle;
    private boolean isResting = false;
    private int totalWorkouts = 1;
    private int currentWorkoutIndex = 0;
    private boolean isRunning = false;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timeLeft > 0 && isRunning) {
                timeLeft--;

                // Broadcast timer update to activity
                Intent broadcastIntent = new Intent(ACTION_TIMER_TICK);
                broadcastIntent.putExtra("timeLeft", timeLeft);
                LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(broadcastIntent);

                updateNotification(null);
                handler.postDelayed(this, 1000);

                Log.d(TAG, "⏱️ Timer tick: " + timeLeft + "s remaining");
            } else if (timeLeft <= 0) {
                // Timer completed - notify activity
                Log.d(TAG, "✅ Timer completed! Notifying activity...");

                Intent broadcastIntent = new Intent(ACTION_WORKOUT_COMPLETE);
                LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(broadcastIntent);

                // Don't stop service - let activity control it
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if ("STOP_SERVICE".equals(action)) {
                Log.d(TAG, "🛑 Stopping service");
                stopTimer();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            workoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
            int newTimeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
            isResting = intent.getBooleanExtra(EXTRA_IS_RESTING, false);
            totalWorkouts = intent.getIntExtra(EXTRA_TOTAL_WORKOUTS, 1);
            currentWorkoutIndex = intent.getIntExtra(EXTRA_CURRENT_WORKOUT_INDEX, 0);

            // Only restart timer if it's a significant change (new workout)
            if (newTimeLeft > 0 && Math.abs(newTimeLeft - timeLeft) > 5) {
                Log.d(TAG, "🔄 New workout/rest period: " + workoutTitle + " (" + newTimeLeft + "s)");
                timeLeft = newTimeLeft;
                totalDuration = newTimeLeft;
                startTimer();
            } else if (newTimeLeft > 0) {
                // Just an update, don't restart timer
                timeLeft = newTimeLeft;
            }
        }

        createNotificationChannel();

        // Update notification immediately with current values
        Notification notification = buildNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }

        // Start as foreground service
        try {
            startForeground(1, notification);
            Log.d(TAG, "✅ Foreground service started");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting foreground service", e);
        }

        // Return STICKY to keep service alive
        return START_STICKY;
    }

    private void startTimer() {
        stopTimer();
        isRunning = true;
        handler.post(timerRunnable);
        Log.d(TAG, "▶️ Timer started");
    }

    private void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        Log.d(TAG, "⏸️ Timer stopped");
    }

    private void updateNotification(String extraText) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = buildPendingIntent();

        Notification notification;

        if (extraText != null) {
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Workout: " + workoutTitle)
                    .setContentText(extraText)
                    .setSmallIcon(R.drawable.notification)
                    .setProgress(0, 0, false)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setContentIntent(pendingIntent)
                    .setColor(Color.YELLOW)
                    .build();
        } else {
            notification = buildNotification();
        }

        if (manager != null) {
            manager.notify(1, notification);
        }
    }

    private PendingIntent buildPendingIntent() {
        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification() {
        int progress = totalDuration > 0 ? 100 - (int)(((float)timeLeft / totalDuration) * 100) : 0;
        PendingIntent pendingIntent = buildPendingIntent();

        String displayTitle = isResting ? "Resting - " + workoutTitle : "Workout: " + workoutTitle;
        String workoutProgress = "";
        if (totalWorkouts > 1) {
            workoutProgress = " (" + (currentWorkoutIndex + 1) + "/" + totalWorkouts + ")";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(displayTitle + workoutProgress)
                .setContentText("Time left: " + formatTime(timeLeft))
                .setSmallIcon(R.drawable.notification)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.YELLOW)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private String formatTime(int totalSecs) {
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Workout Notification Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Shows workout timer progress");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopTimer();
        super.onDestroy();
        Log.d(TAG, "🗑️ Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}