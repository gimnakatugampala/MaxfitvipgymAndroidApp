package com.example.maxfitvipgymapp.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import com.example.maxfitvipgymapp.Activity.MainActivity;
import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

        SharedPreferences prefs = context.getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);
        String lastWorkoutDate = prefs.getString("lastWorkoutDate", "");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().getTime());

        boolean isRestDay = currentDay.equals("Wednesday") || currentDay.equals("Sunday");
        boolean workoutCompletedToday = today.equals(lastWorkoutDate);

        // Update streak number
        views.setTextViewText(R.id.widgetStreakNumber, String.valueOf(currentStreak));

        // Show progress indicator if streak > 0
        if (currentStreak > 0 && !isRestDay && !workoutCompletedToday) {
            views.setViewVisibility(R.id.widgetProgressSection, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widgetProgressSection, View.GONE);
        }

        if (isRestDay) {
            // REST DAY STATE
            views.setTextViewText(R.id.widgetTitle, "Rest Day");
            views.setTextViewText(R.id.widgetEmoji, " 😴");
            views.setTextViewText(R.id.widgetMessage, "Relax and recover today");
            views.setTextViewText(R.id.widgetButtonText, "VIEW SCHEDULE");
            views.setImageViewResource(R.id.widgetIcon, R.drawable.resting);
            views.setImageViewResource(R.id.widgetButtonIcon, R.drawable.ic_view);

            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, mainPendingIntent);

        } else if (workoutCompletedToday) {
            // COMPLETED STATE
            views.setTextViewText(R.id.widgetTitle, "Great Job");
            views.setTextViewText(R.id.widgetEmoji, " 🎉");
            views.setTextViewText(R.id.widgetMessage, "Today's workout completed!");
            views.setTextViewText(R.id.widgetButtonText, "VIEW PROGRESS");
            views.setImageViewResource(R.id.widgetIcon, R.drawable.workout);
            views.setImageViewResource(R.id.widgetButtonIcon, R.drawable.ic_check);

            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, mainPendingIntent);

        } else {
            // WORKOUT PENDING STATE
            views.setTextViewText(R.id.widgetTitle, "Let's Workout");
            views.setTextViewText(R.id.widgetEmoji, " 💪");
            views.setTextViewText(R.id.widgetMessage, getMotivationalMessage(currentStreak));
            views.setTextViewText(R.id.widgetButtonText, "START WORKOUT");
            views.setImageViewResource(R.id.widgetIcon, R.drawable.running);
            views.setImageViewResource(R.id.widgetButtonIcon, R.drawable.ic_play);

            Intent workoutIntent = new Intent(context, WorkoutActivity.class);
            workoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent workoutPendingIntent = PendingIntent.getActivity(context, 0, workoutIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetButton, workoutPendingIntent);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String getMotivationalMessage(int streak) {
        if (streak == 0) {
            return "Start your journey today";
        } else if (streak < 3) {
            return "Keep the momentum going!";
        } else if (streak < 7) {
            return streak + " days strong! Keep it up";
        } else if (streak < 14) {
            return "Amazing " + streak + " day streak!";
        } else if (streak < 30) {
            return "Unstoppable! " + streak + " days!";
        } else {
            return "Legend! " + streak + " days! 🔥";
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