// ========================================================================
// SOLUTION: Move Timer Logic to the ForegroundService
// This ensures the timer continues even when Activity is paused/destroyed
// ========================================================================

// FILE: WorkoutForegroundService.java
// Replace or update your existing service with this implementation

package com.example.maxfitvipgymapp.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.R;

public class WorkoutForegroundService extends Service {

    private static final String TAG = "WorkoutForegroundService";
    private static final String CHANNEL_ID = "workout_foreground_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String EXTRA_WORKOUT_TITLE = "workout_title";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_IS_RESTING = "is_resting";
    public static final String EXTRA_ACTION = "action";

    public static final String ACTION_START_TIMER = "start_timer";
    public static final String ACTION_PAUSE_TIMER = "pause_timer";
    public static final String ACTION_RESUME_TIMER = "resume_timer";
    public static final String ACTION_STOP_SERVICE = "stop_service";

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int timeLeft = 0;
    private String currentWorkoutTitle = "Workout";
    private boolean isResting = false;
    private boolean isTimerRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ Service created");

        // Initialize Handler on main looper for reliable timing
        timerHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "📥 onStartCommand called");

        if (intent != null) {
            String action = intent.getStringExtra(EXTRA_ACTION);

            if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "🛑 Stop service action received");
                stopTimer();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            if (ACTION_PAUSE_TIMER.equals(action)) {
                Log.d(TAG, "⏸️ Pause timer action received");
                pauseTimer();
                return START_STICKY;
            }

            if (ACTION_RESUME_TIMER.equals(action)) {
                Log.d(TAG, "▶️ Resume timer action received");
                resumeTimer();
                return START_STICKY;
            }

            // Update workout info
            currentWorkoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
            timeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
            isResting = intent.getBooleanExtra(EXTRA_IS_RESTING, false);

            if (currentWorkoutTitle == null) {
                currentWorkoutTitle = "Workout";
            }

            Log.d(TAG, "📊 Timer update: " + currentWorkoutTitle + " - " + timeLeft + "s (resting: " + isResting + ")");

            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, buildNotification());

            // Start or restart timer if action is START_TIMER or if timer not running
            if (ACTION_START_TIMER.equals(action) || !isTimerRunning) {
                startTimer();
            } else {
                // Just update the notification
                updateNotification();
            }
        }

        // START_STICKY ensures service restarts if killed by system
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Workout Timer",
                    NotificationManager.IMPORTANCE_LOW // LOW = no sound
            );
            channel.setDescription("Shows workout timer progress");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText;
        if (isResting) {
            contentText = "Rest - " + formatTime(timeLeft);
        } else {
            contentText = currentWorkoutTitle + " - " + formatTime(timeLeft);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Workout in Progress")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Can't be dismissed
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    // ✅ START TIMER - Runs independently of Activity
    private void startTimer() {
        stopTimer(); // Stop any existing timer first

        Log.d(TAG, "⏱️ Starting timer: " + timeLeft + " seconds");
        isTimerRunning = true;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    // Broadcast timer update to Activity
                    Intent intent = new Intent("TIMER_UPDATE");
                    intent.putExtra("timeLeft", timeLeft);
                    intent.putExtra("workoutTitle", currentWorkoutTitle);
                    intent.putExtra("isResting", isResting);
                    LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(intent);

                    // Update notification every 5 seconds (battery optimization)
                    if (timeLeft % 5 == 0) {
                        updateNotification();
                    }

                    timeLeft--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    // Timer completed
                    Log.d(TAG, "✅ Timer completed for: " + currentWorkoutTitle);

                    // Broadcast completion
                    Intent intent = new Intent("TIMER_COMPLETE");
                    intent.putExtra("workoutTitle", currentWorkoutTitle);
                    intent.putExtra("isResting", isResting);
                    LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(intent);

                    isTimerRunning = false;
                }
            }
        };

        // Start the timer
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        isTimerRunning = false;
        Log.d(TAG, "⏹️ Timer stopped");
    }

    private void pauseTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        isTimerRunning = false;
        updateNotification();
        Log.d(TAG, "⏸️ Timer paused");
    }

    private void resumeTimer() {
        if (!isTimerRunning && timeLeft > 0) {
            startTimer();
            Log.d(TAG, "▶️ Timer resumed");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        Log.d(TAG, "🛑 Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}