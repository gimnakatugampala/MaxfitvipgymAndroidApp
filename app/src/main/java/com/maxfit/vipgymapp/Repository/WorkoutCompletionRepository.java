package com.maxfit.vipgymapp.Repository;

import android.util.Log;

import com.maxfit.vipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutCompletionRepository {
    private static final String TAG = "WorkoutCompletionRepo";
    private static final String TABLE_NAME = "workout_completions";
    private SupabaseClient client;

    public WorkoutCompletionRepository() {
        this.client = SupabaseClient.getInstance();
    }

    /**
     * Record a workout completion for today
     */
    public boolean recordWorkoutCompletion(int memberId, int durationMinutes, int workoutsCount) {
        try {
            String today = getTodayDate();

            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("completion_date", today);
            data.put("total_duration_minutes", durationMinutes);
            data.put("workouts_completed", workoutsCount);

            // Try to insert, if exists, update
            String filter = "member_id=eq." + memberId + "&completion_date=eq." + today;

            try {
                // Try to insert first
                JSONArray result = client.insert(TABLE_NAME, data);
                Log.d(TAG, "✅ Workout completion recorded: " + today);
                return result.length() > 0;
            } catch (Exception e) {
                // If insert fails (duplicate), update existing record
                Log.d(TAG, "Record exists, updating...");
                JSONArray result = client.update(TABLE_NAME, filter, data);
                return result.length() > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recording workout completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get workout completion dates for the last N months
     */
    public List<String> getCompletionDates(int memberId, int months) {
        List<String> dates = new ArrayList<>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -months);
            String startDate = formatDate(cal.getTime());

            String filter = "member_id=eq." + memberId
                    + "&completion_date=gte." + startDate
                    + "&order=completion_date.desc";

            JSONArray result = client.select(TABLE_NAME, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                String date = obj.optString("completion_date");
                if (!date.isEmpty()) {
                    dates.add(date);
                }
            }

            Log.d(TAG, "✅ Found " + dates.size() + " completion dates");
        } catch (Exception e) {
            Log.e(TAG, "Error getting completion dates: " + e.getMessage());
        }
        return dates;
    }

    /**
     * Calculate current streak
     */
    public int calculateCurrentStreak(int memberId) {
        try {
            // Get all completion dates in descending order
            String filter = "member_id=eq." + memberId + "&order=completion_date.desc";
            JSONArray result = client.select(TABLE_NAME, filter);

            if (result.length() == 0) {
                return 0;
            }

            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);

            String todayStr = formatDate(today.getTime());
            String yesterdayStr = formatDate(yesterday.getTime());

            // Check if latest completion is today or yesterday
            String latestDate = result.getJSONObject(0).getString("completion_date");

            if (!latestDate.equals(todayStr) && !latestDate.equals(yesterdayStr)) {
                return 0; // Streak broken
            }

            int streak = 0;

            // FIX: Start checking from the DATE OF THE LATEST WORKOUT
            // If we worked out today, check from today. If yesterday, check from yesterday.
            Calendar checkDate = Calendar.getInstance();
            if (latestDate.equals(yesterdayStr)) {
                checkDate.add(Calendar.DAY_OF_MONTH, -1);
            }

            for (int i = 0; i < result.length(); i++) {
                String completionDate = result.getJSONObject(i).getString("completion_date");
                String expectedDate = formatDate(checkDate.getTime());

                if (completionDate.equals(expectedDate)) {
                    streak++;
                    // Move checkDate back by one day for the next iteration
                    checkDate.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    // Gap found, streak ends
                    break;
                }
            }

            Log.d(TAG, "✅ Current streak: " + streak + " days");
            return streak;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating streak: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if workout was completed today
     */
    public boolean isCompletedToday(int memberId) {
        try {
            String today = getTodayDate();
            String filter = "member_id=eq." + memberId + "&completion_date=eq." + today;

            JSONArray result = client.select(TABLE_NAME, filter);
            return result.length() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking today's completion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get workout statistics
     */
    public Map<String, Object> getWorkoutStats(int memberId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            String filter = "member_id=eq." + memberId;
            JSONArray result = client.select(TABLE_NAME, filter);

            int totalWorkouts = 0;
            int totalDuration = 0;

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                totalWorkouts += obj.optInt("workouts_completed", 0);
                totalDuration += obj.optInt("total_duration_minutes", 0);
            }

            stats.put("total_days", result.length());
            stats.put("total_workouts", totalWorkouts);
            stats.put("total_duration_minutes", totalDuration);
            stats.put("current_streak", calculateCurrentStreak(memberId));

        } catch (Exception e) {
            Log.e(TAG, "Error getting workout stats: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Get completion data for a specific month
     */
    public Map<String, Map<String, Object>> getMonthCompletions(int memberId, int year, int month) {
        Map<String, Map<String, Object>> completions = new HashMap<>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            String startDate = formatDate(cal.getTime());

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String endDate = formatDate(cal.getTime());

            String filter = "member_id=eq." + memberId
                    + "&completion_date=gte." + startDate
                    + "&completion_date=lte." + endDate;

            JSONArray result = client.select(TABLE_NAME, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                String date = obj.optString("completion_date");

                Map<String, Object> data = new HashMap<>();
                data.put("duration", obj.optInt("total_duration_minutes"));
                data.put("workouts", obj.optInt("workouts_completed"));

                completions.put(date, data);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting month completions: " + e.getMessage());
        }
        return completions;
    }

    /**
     * ✅ ADDED: Get ALL completion data for momentum charts and heatmaps
     */
    public List<Map<String, Object>> getAllCompletions(int memberId) {
        List<Map<String, Object>> completions = new ArrayList<>();
        try {
            // Get last 365 days
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -1);
            String startDate = formatDate(cal.getTime());

            String filter = "member_id=eq." + memberId + "&completion_date=gte." + startDate;
            JSONArray result = client.select(TABLE_NAME, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("date", obj.optString("completion_date"));
                map.put("duration", obj.optInt("total_duration_minutes"));
                map.put("count", obj.optInt("workouts_completed"));
                completions.add(map);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all completions: " + e.getMessage());
        }
        return completions;
    }

    /**
     * Helper: Get today's date as string
     */
    private String getTodayDate() {
        return formatDate(Calendar.getInstance().getTime());
    }

    /**
     * Helper: Format date as YYYY-MM-DD
     */
    private String formatDate(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }
}