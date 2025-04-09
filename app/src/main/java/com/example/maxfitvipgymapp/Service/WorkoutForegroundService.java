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
    public static final String EXTRA_IS_RESTING = "IS_RESTING"; // ✅ new flag

    private Handler handler = new Handler();
    private int timeLeft;
    private int totalDuration;
    private String workoutTitle;
    private boolean isResting = false; // ✅ default false

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timeLeft > 0) {
                timeLeft--;
                updateNotification(null);  // Update the notification
                handler.postDelayed(this, 1000);  // Continue the countdown
            } else {
                updateNotification("Workout Complete!");
                stopSelf();  // Stop the service when workout time is over
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        workoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
        timeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
        totalDuration = totalDuration == 0 ? timeLeft : totalDuration;
        isResting = intent.getBooleanExtra(EXTRA_IS_RESTING, false); // ✅ read resting flag

        createNotificationChannel();
        startForeground(1, buildNotification());

        handler.removeCallbacks(timerRunnable);
        handler.postDelayed(timerRunnable, 1000);

        return START_NOT_STICKY;
    }

    private void sendTimerUpdateBroadcast(int timeLeft) {
        Intent broadcastIntent = new Intent("com.example.maxfitvipgymapp.TIMER_UPDATE");
        broadcastIntent.putExtra("timeLeft", timeLeft);
        sendBroadcast(broadcastIntent);
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
            notification = buildNotification(); // Uses isResting title logic
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
        int progress = 100 - (int)(((float)timeLeft / totalDuration) * 100);
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
