package com.example.maxfitvipgymapp.Repository;

import android.util.Log;

import com.example.maxfitvipgymapp.Config.SupabaseConfig;
import com.example.maxfitvipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Repository for Real-time Presence Tracking (Optimized)
 *
 * Key Difference from Old Approach:
 * - No heartbeat updates every 15 seconds
 * - Only INSERT when app opens, DELETE when app closes
 * - 99% fewer database operations
 * - Realtime updates handled by Supabase WebSocket
 */
public class PresenceRealtimeRepository {

    private static final String TAG = "PresenceRealtimeRepo";
    // Ensure this constant exists in SupabaseConfig, or replace with "member_presence_realtime"
    private static final String TABLE_NAME = "member_presence_realtime";

    private SupabaseClient client;

    public PresenceRealtimeRepository() {
        this.client = SupabaseClient.getInstance();
    }

    /**
     * Join presence - Called ONCE when app opens
     */
    public boolean joinPresence(int memberId, String firstName, String lastName,
                                String membershipId, String deviceInfo, String appVersion) {
        try {
            Log.d(TAG, "📍 Joining presence for member: " + memberId);

            // First, ensure no duplicate entries (in case of crash)
            leavePresence(memberId);

            // Create presence record
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("first_name", firstName);
            data.put("last_name", lastName);
            data.put("membership_id", membershipId);
            data.put("device_info", deviceInfo);
            data.put("platform", "android");

            if (appVersion != null && !appVersion.isEmpty()) {
                data.put("app_version", appVersion);
            }

            // Insert into database (ONCE)
            JSONArray result = client.insert(TABLE_NAME, data);

            if (result.length() > 0) {
                Log.d(TAG, "✅ Member " + memberId + " joined presence successfully");
                return true;
            } else {
                Log.e(TAG, "❌ Failed to join presence - no result returned");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error joining presence: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Leave presence - Called ONCE when app closes
     */
    public boolean leavePresence(int memberId) {
        try {
            Log.d(TAG, "👋 Leaving presence for member: " + memberId);

            // Delete presence record
            String filter = "member_id=eq." + memberId;
            client.delete(TABLE_NAME, filter);

            Log.d(TAG, "✅ Member " + memberId + " left presence successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error leaving presence: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Refresh presence - Optional fallback (use sparingly)
     */
    public boolean refreshPresence(int memberId, String firstName, String lastName,
                                   String membershipId, String deviceInfo, String appVersion) {
        try {
            Log.d(TAG, "🔄 Refreshing presence for member: " + memberId);

            // Try to update existing record first
            JSONObject updateData = new JSONObject();
            updateData.put("joined_at", getCurrentTimestamp());
            updateData.put("device_info", deviceInfo);

            if (appVersion != null && !appVersion.isEmpty()) {
                updateData.put("app_version", appVersion);
            }

            String filter = "member_id=eq." + memberId;

            // ✅ FIXED: Correct argument order (Table, Filter, Data)
            JSONArray result = client.update(TABLE_NAME, filter, updateData);

            if (result.length() > 0) {
                Log.d(TAG, "✅ Presence refreshed successfully");
                return true;
            } else {
                // If update failed (no record exists), insert new one
                Log.d(TAG, "⚠️ No existing presence found, inserting new record");
                return joinPresence(memberId, firstName, lastName, membershipId, deviceInfo, appVersion);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error refreshing presence: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current online member count (for testing/debugging)
     */
    public int getOnlineCount() {
        try {
            // ✅ FIXED: select takes (table, filterString)
            JSONArray result = client.select(TABLE_NAME, "select=*");
            int count = result.length();
            Log.d(TAG, "📊 Current online count: " + count);
            return count;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting online count: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Check if a specific member is currently online
     */
    public boolean isMemberOnline(int memberId) {
        try {
            // ✅ FIXED: Construct proper query string
            String filter = "select=*&member_id=eq." + memberId;
            JSONArray result = client.select(TABLE_NAME, filter);
            boolean isOnline = result.length() > 0;
            Log.d(TAG, "🔍 Member " + memberId + " online status: " + isOnline);
            return isOnline;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking member online status: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get list of all currently online members (for testing)
     */
    public JSONArray getOnlineMembers() {
        try {
            // ✅ FIXED: Construct proper query string with ordering
            String query = "select=*&order=joined_at.desc";
            JSONArray result = client.select(TABLE_NAME, query);
            Log.d(TAG, "📋 Retrieved " + result.length() + " online members");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting online members: " + e.getMessage(), e);
            return new JSONArray();
        }
    }

    /**
     * Force cleanup for a specific member (emergency use)
     */
    public boolean forceCleanup(int memberId) {
        Log.w(TAG, "🧹 Force cleanup requested for member: " + memberId);
        return leavePresence(memberId);
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        // Format: 2024-12-21T10:30:00Z
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date());
    }

    /**
     * Test connection to presence table
     */
    public boolean testConnection() {
        try {
            // ✅ FIXED: Construct proper query string
            String query = "select=id&limit=1";
            JSONArray result = client.select(TABLE_NAME, query);
            Log.d(TAG, "✅ Connection test successful");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Connection test failed: " + e.getMessage(), e);
            return false;
        }
    }
}