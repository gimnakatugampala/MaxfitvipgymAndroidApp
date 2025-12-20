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

public class ProgressRepository {
    private static final String TAG = "ProgressRepository";
    private SupabaseClient client;

    public ProgressRepository() {
        this.client = SupabaseClient.getInstance();
    }

    // Add weight progress
    public boolean addWeightProgress(int memberId, double weight) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("weight_value", weight);
            data.put("weight_kg", String.valueOf(weight));

            client.insert(SupabaseConfig.TABLE_WEIGHT_PROGRESS, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding weight progress: " + e.getMessage());
            return false;
        }
    }

    // Get weight progress for member
    public List<Map<String, Object>> getWeightProgress(int memberId, int limit) {
        List<Map<String, Object>> progressList = new ArrayList<>();
        try {
            String filter = "member_id=eq." + memberId + "&order=date.desc&limit=" + limit;
            JSONArray result = client.select(SupabaseConfig.TABLE_WEIGHT_PROGRESS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> progress = new HashMap<>();
                progress.put("date", obj.optString("date"));
                progress.put("value", obj.optDouble("weight_value"));
                progressList.add(progress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting weight progress: " + e.getMessage());
        }
        return progressList;
    }

    // Add bicep progress
    public boolean addBicepProgress(int memberId, double bicepSize) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("bicep_value", bicepSize);
            data.put("bicep_size_inch", String.valueOf(bicepSize));

            client.insert(SupabaseConfig.TABLE_BICEP_PROGRESS, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding bicep progress: " + e.getMessage());
            return false;
        }
    }

    // Get bicep progress
    public List<Map<String, Object>> getBicepProgress(int memberId, int limit) {
        List<Map<String, Object>> progressList = new ArrayList<>();
        try {
            String filter = "member_id=eq." + memberId + "&order=date.desc&limit=" + limit;
            JSONArray result = client.select(SupabaseConfig.TABLE_BICEP_PROGRESS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> progress = new HashMap<>();
                progress.put("date", obj.optString("date"));
                progress.put("value", obj.optDouble("bicep_value"));
                progressList.add(progress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting bicep progress: " + e.getMessage());
        }
        return progressList;
    }

    // Add chest progress
    public boolean addChestProgress(int memberId, double chestSize) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("chest_value", chestSize);
            data.put("chest_size_inch", String.valueOf(chestSize));

            client.insert(SupabaseConfig.TABLE_CHEST_PROGRESS, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding chest progress: " + e.getMessage());
            return false;
        }
    }

    // Get chest progress
    public List<Map<String, Object>> getChestProgress(int memberId, int limit) {
        List<Map<String, Object>> progressList = new ArrayList<>();
        try {
            String filter = "member_id=eq." + memberId + "&order=date.desc&limit=" + limit;
            JSONArray result = client.select(SupabaseConfig.TABLE_CHEST_PROGRESS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> progress = new HashMap<>();
                progress.put("date", obj.optString("date"));
                progress.put("value", obj.optDouble("chest_value"));
                progressList.add(progress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting chest progress: " + e.getMessage());
        }
        return progressList;
    }

    // Add hip progress
    public boolean addHipProgress(int memberId, double hipSize) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("hip_value", hipSize);
            data.put("hip_size_inch", String.valueOf(hipSize));

            client.insert(SupabaseConfig.TABLE_HIP_SIZE_PROGRESS, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding hip progress: " + e.getMessage());
            return false;
        }
    }

    // Get hip progress
    public List<Map<String, Object>> getHipProgress(int memberId, int limit) {
        List<Map<String, Object>> progressList = new ArrayList<>();
        try {
            String filter = "member_id=eq." + memberId + "&order=date.desc&limit=" + limit;
            JSONArray result = client.select(SupabaseConfig.TABLE_HIP_SIZE_PROGRESS, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> progress = new HashMap<>();
                progress.put("date", obj.optString("date"));
                progress.put("value", obj.optDouble("hip_value"));
                progressList.add(progress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting hip progress: " + e.getMessage());
        }
        return progressList;
    }

    // Get latest stats for member
    public Map<String, Double> getLatestStats(int memberId) {
        Map<String, Double> stats = new HashMap<>();

        try {
            // Get latest weight
            List<Map<String, Object>> weightProgress = getWeightProgress(memberId, 1);
            if (!weightProgress.isEmpty()) {
                stats.put("weight", (Double) weightProgress.get(0).get("value"));
            }

            // Get latest bicep
            List<Map<String, Object>> bicepProgress = getBicepProgress(memberId, 1);
            if (!bicepProgress.isEmpty()) {
                stats.put("bicep", (Double) bicepProgress.get(0).get("value"));
            }

            // Get latest chest
            List<Map<String, Object>> chestProgress = getChestProgress(memberId, 1);
            if (!chestProgress.isEmpty()) {
                stats.put("chest", (Double) chestProgress.get(0).get("value"));
            }

            // Get latest hip
            List<Map<String, Object>> hipProgress = getHipProgress(memberId, 1);
            if (!hipProgress.isEmpty()) {
                stats.put("hip", (Double) hipProgress.get(0).get("value"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting latest stats: " + e.getMessage());
        }

        return stats;
    }
}