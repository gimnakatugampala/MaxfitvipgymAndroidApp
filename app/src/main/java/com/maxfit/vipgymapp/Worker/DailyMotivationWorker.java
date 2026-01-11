package com.maxfit.vipgymapp.Worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.maxfit.vipgymapp.Activity.MainActivity;
import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.AttendanceRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;

import java.util.Random;

public class DailyMotivationWorker extends Worker {

    private static final String TAG = "DailyMotivationWorker";
    private static final String CHANNEL_ID = "maxfit_motivation_channel";
    private static final int NOTIFICATION_ID = 2001;

    // Creative messages with %s placeholder for the user's name
    private static final String[] MESSAGES = {
            "🚀 Hey %s, the only bad workout is the one that didn't happen!",
            "💪 %s, don't let your goals wait. Crush them today at MaxFit!",
            "🔥 Your future self is begging you to go to the gym, %s. Let's go!",
            "👀 We missed you today, %s! Come get those gains.",
            "🏋️ Consistency is the code to success, %s. Keep your streak alive!",
            "⚡ %s, you didn't come this far to only come this far. Hit the gym!",
            "🛑 Stop scrolling, start lifting, %s! See you at MaxFit?",
            "🏆 Champions like you are made when no one is watching, %s. Time to train!",
            "💡 Stressful day, %s? The gym is the best therapy. Come sweat it out.",
            "🕒 It's not too late to change your day, %s. One hour -> 4%% of your day.", // %% escapes the percent sign
            "💥 %s, sweat is just your fat crying. Make it weep today!",
            "🦁 Be a beast today, %s. The gym is your jungle.",
            "📅 One day or Day One? You decide today, %s.",
            "🔋 %s, time to recharge your body and mind at MaxFit.",
            "🚧 Excuses don't burn calories, %s. Let's get moving!",
            "👟 Lace up, %s! The weights are calling your name.",
            "💎 Diamond pressure makes diamonds, %s. Let's build that strength!",
            "🎯 Stay focused, %s. Remember why you started.",
            "🌟 %s, do something today that your future self will thank you for.",
            "😈 Beat the old you, %s. That's the only competition that matters."
    };

    public DailyMotivationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔔 DailyMotivationWorker started");

        Context context = getApplicationContext();
        SessionManager sessionManager = new SessionManager(context);

        // 1. Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            return Result.success();
        }

        try {
            // 2. Check if they have already attended today
            AttendanceRepository repository = new AttendanceRepository();
            int memberId = sessionManager.getMemberId();
            boolean hasAttended = repository.hasAttendedToday(memberId);

            if (hasAttended) {
                Log.d(TAG, "✅ Member already attended today. No notification needed.");
            } else {
                // 3. Get User Name
                Member member = sessionManager.getMemberData();
                String firstName = "Champion"; // Default fallback
                if (member != null && member.getFirstName() != null && !member.getFirstName().isEmpty()) {
                    firstName = member.getFirstName();
                }

                Log.d(TAG, "⚠️ Member has NOT attended today. Sending motivation to " + firstName);
                sendMotivationalNotification(context, firstName);
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in DailyMotivationWorker: " + e.getMessage());
            return Result.retry();
        }
    }

    private void sendMotivationalNotification(Context context, String firstName) {
        createNotificationChannel(context);

        // Pick a random message
        String rawMessage = MESSAGES[new Random().nextInt(MESSAGES.length)];

        // Format the message with the user's name
        String finalMessage = String.format(rawMessage, firstName);

        // Intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("navigateTo", "home");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this resource exists
                .setContentTitle("MaxFit Motivation ⚡")
                .setContentText(finalMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(finalMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // Permission check is usually handled in Activity; assuming permission granted here
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing for notifications: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Daily Motivation";
            String description = "Reminders to hit the gym";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}