package com.maxfit.vipgymapp.Worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.maxfit.vipgymapp.Repository.HealthRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DailySyncWorker extends Worker {

    private static final String TAG = "DailySyncWorker";
    private static final String PREFS_NAME = "HealthTrackerPrefs";

    public DailySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 DailySyncWorker started...");

        Context context = getApplicationContext();
        SessionManager sessionManager = new SessionManager(context);
        int memberId = sessionManager.getMemberId();

        if (memberId == -1) {
            Log.e(TAG, "❌ No user logged in, cancelling sync.");
            return Result.failure();
        }

        // 1. Get stored data
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastSavedDate = prefs.getString("last_date", "");
        int savedSteps = prefs.getInt("today_steps", 0);

        // Get today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        // 2. Only upload if we have data from a PREVIOUS day (yesterday)
        // If lastSavedDate == today, we are still collecting data, so don't finalize it yet.
        if (!lastSavedDate.isEmpty() && !lastSavedDate.equals(today)) {

            Log.d(TAG, "📅 Found unsynced data for " + lastSavedDate + ". Uploading...");

            // Calculate metrics (simplified logic matching Service)
            // (You could also store these in Prefs to be more accurate, but calculating here is okay)
            int heightCm = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE).getInt("height_cm", 170);
            double strideLength = (heightCm / 100.0) * 0.415;
            double distanceKm = (savedSteps * strideLength) / 1000.0;
            int activeMinutes = savedSteps / 100;

            // Rough calorie calc (Base BMR ~1500 + active)
            int calories = 1500 + (int)(savedSteps * 0.04);

            // 3. Upload to Supabase
            HealthRepository repository = new HealthRepository();
            repository.saveDailyStats(memberId, lastSavedDate, savedSteps, distanceKm, calories, activeMinutes);

            return Result.success();
        }

        Log.d(TAG, "✅ No past data to sync (Last date is today).");
        return Result.success();
    }
}