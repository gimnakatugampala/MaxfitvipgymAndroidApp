package com.maxfit.vipgymapp.Repository;

import android.util.Log;
import com.maxfit.vipgymapp.Network.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class HealthRepository {
    private static final String TAG = "HealthRepository";
    private static final String TABLE_NAME = "member_health_stats";
    private SupabaseClient client;

    public HealthRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public void saveDailyStats(int memberId, String date, int steps, double distance, int calories, int activeMinutes) {
        try {
            // 1. Check if record exists for this Member + Date
            String filter = "member_id=eq." + memberId + "&date=eq." + date;
            JSONArray existing = client.select(TABLE_NAME, filter);

            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("date", date);
            data.put("steps", steps);
            data.put("distance_km", distance);
            data.put("calories", calories);
            data.put("active_minutes", activeMinutes);

            if (existing.length() > 0) {
                // 2. Update existing record
                client.update(TABLE_NAME, filter, data);
                Log.d(TAG, "✅ Updated daily stats for: " + date);
            } else {
                // 3. Insert new record
                client.insert(TABLE_NAME, data);
                Log.d(TAG, "✅ Inserted daily stats for: " + date);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving daily stats: " + e.getMessage());
        }
    }
}