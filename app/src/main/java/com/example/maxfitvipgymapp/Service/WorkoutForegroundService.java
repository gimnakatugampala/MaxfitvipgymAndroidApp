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

    private Handler handler = new Handler();
    private int timeLeft;
    private int totalDuration;
    private String workoutTitle;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timeLeft > 0) {
                timeLeft--;
                updateNotification();
                handler.postDelayed(this, 1000);
            } else {
                stopSelf();  // Stop the service when workout time is over
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        workoutTitle = intent.getStringExtra(EXTRA_WORKOUT_TITLE);
        timeLeft = intent.getIntExtra(EXTRA_DURATION, 0);
        totalDuration = timeLeft;

        createNotificationChannel();  // Create the notification channel

        // Start the foreground service and show the notification
        startForeground(1, buildNotification());

        // Start the timer for the service
        handler.post(timerRunnable);

        // Send broadcast to update app's UI (optional, if needed)
        sendTimerUpdateBroadcast(timeLeft);

        return START_NOT_STICKY;  // Service should not restart if killed
    }

    private void sendTimerUpdateBroadcast(int timeLeft) {
        Intent broadcastIntent = new Intent("com.example.maxfitvipgymapp.TIMER_UPDATE");
        broadcastIntent.putExtra("timeLeft", timeLeft);
        sendBroadcast(broadcastIntent);  // Send broadcast to update the app's timer
    }


    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, buildNotification());  // Update notification
    }

    private Notification buildNotification() {
        int progress = 100 - (int)(((float)timeLeft / totalDuration) * 100);

        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Workout: " + workoutTitle)
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
                    "Workout Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Shows workout timer progress");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(timerRunnable);  // Stop the timer when the service is destroyed
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
