package com.example.maxfitvipgymapp.Repository;

import android.util.Log;

import com.example.maxfitvipgymapp.Config.SupabaseConfig;
import com.example.maxfitvipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutRepository {
    private static final String TAG = "WorkoutRepository";
    private SupabaseClient client;

    public WorkoutRepository() {
        this.client = SupabaseClient.getInstance();
    }

    // Get all workouts
    public List<Map<String, Object>> getAllWorkouts() {
        List<Map<String, Object>> workouts = new ArrayList<>();
        try {
            String filter = "is_deleted=eq.false&order=name.asc";
            JSONArray result = client.select(SupabaseConfig.TABLE_WORKOUT, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> workout = new HashMap<>();
                workout.put("id", obj.optInt("id"));
                workout.put("name", obj.optString("name"));
                workout.put("description", obj.optString("description"));
                workout.put("image_url", obj.optString("image_url"));
                workout.put("sets", obj.optInt("sets", 3));
                workout.put("reps", obj.optInt("reps", 10));
                workout.put("duration", obj.optInt("duration", 0));
                workout.put("workout_type_id", obj.optInt("workout_type_id"));
                workouts.add(workout);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting workouts: " + e.getMessage());
        }
        return workouts;
    }

    // Get workout by ID
    public Map<String, Object> getWorkoutById(int workoutId) {
        try {
            String filter = "id=eq." + workoutId;
            JSONArray result = client.select(SupabaseConfig.TABLE_WORKOUT, filter);

            if (result.length() > 0) {
                JSONObject obj = result.getJSONObject(0);
                Map<String, Object> workout = new HashMap<>();
                workout.put("id", obj.optInt("id"));
                workout.put("name", obj.optString("name"));
                workout.put("description", obj.optString("description"));
                workout.put("image_url", obj.optString("image_url"));
                workout.put("sets", obj.optInt("sets", 3));
                workout.put("reps", obj.optInt("reps", 10));
                workout.put("duration", obj.optInt("duration", 0));
                return workout;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting workout: " + e.getMessage());
        }
        return null;
    }

    // Get member's current workout schedule
    public Map<String, Object> getMemberWorkoutSchedule(int memberId) {
        try {
            String filter = "member_id=eq." + memberId + "&is_active=eq.true";
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBER_WORKOUT_SCHEDULE, filter);

            if (result.length() > 0) {
                JSONObject obj = result.getJSONObject(0);
                Map<String, Object> schedule = new HashMap<>();
                schedule.put("id", obj.optInt("id"));
                schedule.put("schedule_id", obj.optInt("schedule_id"));
                schedule.put("start_date", obj.optString("start_date"));
                schedule.put("end_date", obj.optString("end_date"));
                return schedule;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting member workout schedule: " + e.getMessage());
        }
        return null;
    }

    // Get workout schedule details for a day
    public List<Map<String, Object>> getWorkoutScheduleForDay(int scheduleId, String day) {
        List<Map<String, Object>> workouts = new ArrayList<>();
        try {
            String filter = "schedule_id=eq." + scheduleId + "&day=eq." + day + "&is_deleted=eq.false&order=order_index.asc";
            JSONArray result = client.select(SupabaseConfig.TABLE_WORKOUT_SCHEDULE_DETAILS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> workout = new HashMap<>();
                workout.put("id", obj.optInt("id"));
                workout.put("workout_id", obj.optInt("workout_id"));
                workout.put("set_no", obj.optString("set_no"));
                workout.put("rep_no", obj.optString("rep_no"));
                workout.put("duration_minutes", obj.optString("duration_minutes"));
                workout.put("order_index", obj.optInt("order_index"));
                workout.put("is_rest_day", obj.optBoolean("is_rest_day"));

                // Get workout details
                int workoutId = obj.optInt("workout_id");
                Map<String, Object> workoutDetails = getWorkoutById(workoutId);
                if (workoutDetails != null) {
                    workout.putAll(workoutDetails);
                }

                workouts.add(workout);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting workout schedule for day: " + e.getMessage());
        }
        return workouts;
    }

    // Get member's workout schedule details
    public List<Map<String, Object>> getMemberWorkoutScheduleDetails(int memberScheduleId, String day) {
        List<Map<String, Object>> workouts = new ArrayList<>();
        try {
            String filter = "member_schedule_id=eq." + memberScheduleId + "&day=eq." + day + "&order=order_index.asc";
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBER_WORKOUT_SCHEDULE_DETAILS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> workout = new HashMap<>();
                workout.put("id", obj.optInt("id"));
                workout.put("workout_id", obj.optInt("workout_id"));
                workout.put("set_no", obj.optString("set_no"));
                workout.put("rep_no", obj.optString("rep_no"));
                workout.put("duration_minutes", obj.optString("duration_minutes"));
                workout.put("completed", obj.optBoolean("completed"));
                workout.put("is_rest_day", obj.optBoolean("is_rest_day"));

                // Get workout details
                int workoutId = obj.optInt("workout_id");
                Map<String, Object> workoutDetails = getWorkoutById(workoutId);
                if (workoutDetails != null) {
                    workout.putAll(workoutDetails);
                }

                workouts.add(workout);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting member workout schedule details: " + e.getMessage());
        }
        return workouts;
    }

    // Mark workout as completed
    public boolean markWorkoutCompleted(int detailId) {
        try {
            String filter = "id=eq." + detailId;
            JSONObject data = new JSONObject();
            data.put("completed", true);

            client.update(SupabaseConfig.TABLE_MEMBER_WORKOUT_SCHEDULE_DETAILS, filter, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking workout completed: " + e.getMessage());
            return false;
        }
    }

    // Get workout videos
    public List<String> getWorkoutVideos(int workoutId) {
        List<String> videos = new ArrayList<>();
        try {
            String filter = "workout_id=eq." + workoutId + "&is_deleted=eq.false";
            JSONArray result = client.select("workout_video", filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                String videoUrl = obj.optString("video_url");
                if (!videoUrl.isEmpty()) {
                    videos.add(videoUrl);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting workout videos: " + e.getMessage());
        }
        return videos;
    }
}