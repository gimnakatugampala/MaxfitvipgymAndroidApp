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

/**
 * DailySyncWorker - Automatically uploads health data at end of day
 *
 * Runs daily at midnight (or when device wakes up) to:
 * 1. Upload yesterday's completed data to database
 * 2. Handle any missed days from previous periods
 *
 * This ensures ALL user health data is saved even if app is closed.
 */
public class DailySyncWorker extends Worker {

    private static final String TAG = "DailySyncWorker";
    private static final String PREFS_NAME = "HealthTrackerPrefs";
    private static final String USER_PROFILE_PREFS = "UserProfile";

    public DailySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 DailySyncWorker started - Auto-saving health data");

        Context context = getApplicationContext();
        SessionManager sessionManager = new SessionManager(context);
        int memberId = sessionManager.getMemberId();

        if (memberId == -1) {
            Log.e(TAG, "❌ No user logged in, cannot sync health data");
            return Result.failure();
        }

        try {
            // Upload any previous day's data
            uploadPreviousDayData(context, memberId);

            Log.d(TAG, "✅ DailySyncWorker completed successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in DailySyncWorker: " + e.getMessage(), e);
            // Retry on failure (WorkManager will automatically retry)
            return Result.retry();
        }
    }

    /**
     * Uploads health data for any completed previous day
     */
    private void uploadPreviousDayData(Context context, int memberId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String lastSavedDate = prefs.getString("last_date", "");
        String today = getTodayDate();

        Log.d(TAG, "📅 Checking data - Last saved: " + lastSavedDate + ", Today: " + today);

        // Only upload if we have data from a PREVIOUS day (not today)
        if (lastSavedDate.isEmpty()) {
            Log.d(TAG, "✅ No previous data to sync (first run or already synced)");
            return;
        }

        if (lastSavedDate.equals(today)) {
            Log.d(TAG, "✅ Data is for today - will sync tomorrow");
            return;
        }

        // We have data from yesterday (or earlier) - upload it!
        int savedSteps = prefs.getInt("today_steps", 0);
        long savedActiveTimeMs = prefs.getLong("total_active_time_ms", 0);

        Log.d(TAG, "📊 Uploading data for " + lastSavedDate +
                " - Steps: " + savedSteps +
                ", Active time: " + (savedActiveTimeMs / 60000) + " min");

        // Calculate derived metrics using same logic as HealthTrackerService
        SharedPreferences userPrefs = context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE);
        int userHeightCm = userPrefs.getInt("height_cm", 170);
        double userWeightKg = userPrefs.getFloat("weight_kg", 70.0f);
        int userAge = userPrefs.getInt("age", 30);
        String userGender = userPrefs.getString("gender", "M");

        // Calculate distance
        double heightMeters = userHeightCm / 100.0;
        double strideLength = userGender.equals("M") ? heightMeters * 0.415 : heightMeters * 0.413;
        double distanceKm = (savedSteps * strideLength) / 1000.0;

        // Calculate calories using BMR + activity
        double bmr;
        if (userGender.equals("M")) {
            bmr = 88.362 + (13.397 * userWeightKg) + (4.799 * userHeightCm) - (5.677 * userAge);
        } else {
            bmr = 447.593 + (9.247 * userWeightKg) + (3.098 * userHeightCm) - (4.330 * userAge);
        }
        int calories = (int) (bmr + (savedSteps * 0.04));

        // Calculate active minutes
        int activeMinutes = (int) (savedActiveTimeMs / 60000);

        Log.d(TAG, "📊 Calculated metrics - Distance: " + String.format("%.2f", distanceKm) + " km, " +
                "Calories: " + calories + ", Active: " + activeMinutes + " min");

        // Upload to Supabase database
        HealthRepository repository = new HealthRepository();
        repository.saveDailyStats(memberId, lastSavedDate, savedSteps, distanceKm, calories, activeMinutes);

        Log.d(TAG, "✅ Successfully uploaded health data for " + lastSavedDate);

        // Clear the saved data since it's now uploaded
        // (The service will create fresh data for the new day)
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_date", today);
        editor.putInt("today_steps", 0);
        editor.putInt("sensor_baseline", -1);
        editor.putLong("total_active_time_ms", 0);
        editor.apply();

        Log.d(TAG, "✅ Reset local storage for new day");
    }

    /**
     * Get today's date in YYYY-MM-DD format
     */
    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}