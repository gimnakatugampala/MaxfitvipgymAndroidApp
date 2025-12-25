package com.maxfit.vipgymapp.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.maxfit.vipgymapp.Activity.MainActivity;
import com.maxfit.vipgymapp.Activity.WorkoutActivity;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.WorkoutCompletionRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_START_WORKOUT = "com.example.maxfitvipgymapp.START_WORKOUT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_START_WORKOUT.equals(intent.getAction())) {
            Intent workoutIntent = new Intent(context, WorkoutActivity.class);
            workoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(workoutIntent);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_workout);

        // ✅ Get streak from database in background
        SessionManager sessionManager = new SessionManager(context);
        int memberId = sessionManager.getMemberId();

        if (memberId != -1) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                WorkoutCompletionRepository repository = new WorkoutCompletionRepository();
                int currentStreak = repository.calculateCurrentStreak(memberId);
                boolean workoutCompletedToday = repository.isCompletedToday(memberId);

                // Update UI on main thread
                new android.os.Handler(context.getMainLooper()).post(() -> {
                    updateWidgetViews(context, views, appWidgetManager, appWidgetId,
                            currentStreak, workoutCompletedToday);
                });
            });
            executor.shutdown();
        } else {
            // No user logged in, show default
            updateWidgetViews(context, views, appWidgetManager, appWidgetId, 0, false);
        }
    }

    private static void updateWidgetViews(Context context, RemoteViews views,
                                          AppWidgetManager appWidgetManager, int appWidgetId,
                                          int currentStreak, boolean workoutCompletedToday) {
        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().getTime());
        boolean isRestDay = currentDay.equals("Wednesday") || currentDay.equals("Sunday");

        // 1. Set Streak
        views.setTextViewText(R.id.widgetStreakNumber, String.valueOf(currentStreak));

        // 2. Set Date (New ID in your XML)
        String dateText = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(new Date());
        views.setTextViewText(R.id.widgetDate, dateText);

        if (isRestDay) {
            views.setTextViewText(R.id.widgetTitle, "Rest Day 😴");
            views.setTextViewText(R.id.widgetMessage, "Relax today!");
            views.setImageViewResource(R.id.widgetIcon, R.drawable.resting);

            // Button Click -> Main Activity (View status)
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, mainPendingIntent);

        } else if (workoutCompletedToday) {
            views.setTextViewText(R.id.widgetTitle, "Great Job! 🎉");
            views.setTextViewText(R.id.widgetMessage, "Workout completed!");
            views.setImageViewResource(R.id.widgetIcon, R.drawable.workout);

            // Button Click -> Main Activity (View status)
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, mainPendingIntent);

        } else {
            views.setTextViewText(R.id.widgetTitle, "Let's Workout! 💪");
            views.setTextViewText(R.id.widgetMessage, getShortMessage(currentStreak));
            views.setImageViewResource(R.id.widgetIcon, R.drawable.running);

            // Button Click -> Start Workout Activity
            Intent workoutIntent = new Intent(context, WorkoutActivity.class);
            workoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent workoutPendingIntent = PendingIntent.getActivity(context, 0, workoutIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, workoutPendingIntent);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String getShortMessage(int streak) {
        if (streak == 0) {
            return "Start today!";
        } else if (streak < 3) {
            return "Keep going!";
        } else if (streak < 7) {
            return streak + " days strong!";
        } else if (streak < 14) {
            return streak + " day streak!";
        } else {
            return "🔥 " + streak + " days!";
        }
    }

    public static void updateWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WorkoutWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}