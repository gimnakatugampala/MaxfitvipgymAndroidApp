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
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

// ✅ Import MediaStyle for enhanced controls
import androidx.media.app.NotificationCompat.MediaStyle;

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

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ Service created");

        timerHandler = new Handler(Looper.getMainLooper());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MaxFit:WorkoutWakeLock");
        wakeLock.acquire(4 * 60 * 60 * 1000); // 4 hours max

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra(EXTRA_ACTION);

            // ✅ Handle Notification Action Buttons
            if (intent.getAction() != null) {
                action = intent.getAction();
            }

            if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "🛑 Stop service action received");
                stopTimer();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            if (ACTION_PAUSE_TIMER.equals(action)) {
                pauseTimer();
                return START_STICKY;
            }

            if (ACTION_RESUME_TIMER.equals(action)) {
                resumeTimer();
                return START_STICKY;
            }

            // Update workout info from extras
            if (intent.hasExtra(EXTRA_WORKOUT_TITLE)) {
                currentWorkoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
            }
            if (intent.hasExtra(EXTRA_DURATION)) {
                timeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
            }
            if (intent.hasExtra(EXTRA_IS_RESTING)) {
                isResting = intent.getBooleanExtra(EXTRA_IS_RESTING, false);
            }

            if (currentWorkoutTitle == null) {
                currentWorkoutTitle = "Workout";
            }

            // Start foreground immediately
            startForeground(NOTIFICATION_ID, buildNotification());

            if (ACTION_START_TIMER.equals(action)) {
                startTimer();
            } else if (!isTimerRunning && timeLeft > 0) {
                // Auto-resume if simply started with time remaining
                startTimer();
            } else {
                updateNotification();
            }
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Workout Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows workout timer progress");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ✅ ENHANCED NOTIFICATION BUILDER
    private Notification buildNotification() {
        // 1. Intent to open Activity when clicking the notification body
        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. Prepare Action Intents (Pause, Resume, Stop)
        Intent pauseIntent = new Intent(this, WorkoutForegroundService.class).setAction(ACTION_PAUSE_TIMER);
        PendingIntent pPause = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent resumeIntent = new Intent(this, WorkoutForegroundService.class).setAction(ACTION_RESUME_TIMER);
        PendingIntent pResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, WorkoutForegroundService.class).setAction(ACTION_STOP_SERVICE);
        PendingIntent pStop = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        // 3. Define content text
        String contentText;
        String titleText;
        if (isResting) {
            titleText = "Rest Period";
            contentText = "Next: " + currentWorkoutTitle + " (" + formatTime(timeLeft) + ")";
        } else {
            titleText = currentWorkoutTitle;
            contentText = "Time Remaining: " + formatTime(timeLeft);
        }

        // 4. Build Notification with MediaStyle
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true) // Prevents sound/vibration on every second update
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)

                // ✅ Add MediaStyle
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1)); // Show first 2 actions (Play/Pause + Stop) in compact view

        // 5. Add Actions Dynamically
        // Action 0: Play or Pause
        if (isTimerRunning) {
            // currently running -> show Pause button
            builder.addAction(R.drawable.pause, "Pause", pPause);
        } else {
            // currently paused -> show Resume button
            builder.addAction(R.drawable.playbutton, "Resume", pResume);
        }

        // Action 1: Stop
        builder.addAction(R.drawable.stopbutton, "Stop", pStop);

        return builder.build();
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

    private void startTimer() {
        stopTimer();
        isTimerRunning = true;
        Log.d(TAG, "⏱️ Timer started: " + timeLeft);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Broadcast update
                Intent intent = new Intent("TIMER_UPDATE");
                intent.putExtra("timeLeft", timeLeft);
                intent.putExtra("workoutTitle", currentWorkoutTitle);
                intent.putExtra("isResting", isResting);
                LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(intent);

                // Update notification UI
                updateNotification();

                if (timeLeft > 0) {
                    timeLeft--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    Log.d(TAG, "✅ Timer completed");
                    isTimerRunning = false;

                    Intent completeIntent = new Intent("TIMER_COMPLETE");
                    completeIntent.putExtra("workoutTitle", currentWorkoutTitle);
                    completeIntent.putExtra("isResting", isResting);
                    LocalBroadcastManager.getInstance(WorkoutForegroundService.this).sendBroadcast(completeIntent);

                    // Update notification one last time to show 0:00 or "Complete"
                    updateNotification();
                }
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        isTimerRunning = false;
    }

    private void pauseTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        isTimerRunning = false;
        updateNotification(); // Will switch icon to "Play"
    }

    private void resumeTimer() {
        if (!isTimerRunning && timeLeft > 0) {
            startTimer(); // Will switch icon to "Pause"
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        Log.d(TAG, "🛑 Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}