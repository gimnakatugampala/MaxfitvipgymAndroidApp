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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.R;

public class WorkoutForegroundService extends android.app.Service {

    public static final String CHANNEL_ID = "WorkoutNotificationChannel";
    public static final String EXTRA_WORKOUT_TITLE = "WORKOUT_TITLE";
    public static final String EXTRA_DURATION = "WORKOUT_DURATION";
    public static final String EXTRA_IS_RESTING = "IS_RESTING";

    private Handler handler = new Handler();
    private int timeLeft;
    private int totalDuration;
    private String workoutTitle;
    private boolean isResting = false;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timeLeft > 0) {
                timeLeft--;
                updateNotification(null);
                handler.postDelayed(this, 1000);
            } else {
                updateNotification("Workout Complete!");
                stopSelf();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            workoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
            timeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
            isResting = intent.getBooleanExtra(EXTRA_IS_RESTING, false);

            // Update total duration only when it's a new workout (not just an update)
            if (totalDuration == 0 || timeLeft > totalDuration) {
                totalDuration = timeLeft;
            }
        }

        createNotificationChannel();

        // Update notification immediately with current values
        Notification notification = buildNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }

        // Only start foreground service if not already started
        try {
            startForeground(1, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Don't run internal timer - just update notification when activity tells us
        handler.removeCallbacks(timerRunnable);

        return START_NOT_STICKY;
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
        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildNotification() {
        int progress = totalDuration > 0 ? 100 - (int)(((float)timeLeft / totalDuration) * 100) : 0;
        PendingIntent pendingIntent = buildPendingIntent();

        String displayTitle = isResting ? "Resting - " + workoutTitle : "Workout: " + workoutTitle;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(displayTitle)
                .setContentText("Time left: " + formatTime(timeLeft))
                .setSmallIcon(R.drawable.notification)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.YELLOW)
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
        handler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}