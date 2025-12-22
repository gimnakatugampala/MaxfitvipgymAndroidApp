package com.maxfit.vipgymapp.Repository;

import android.util.Log;

import com.maxfit.vipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PresenceRepository {
    private static final String TAG = "PresenceRepository";
    private static final String TABLE_NAME = "member_presence";
    private SupabaseClient client;

    public PresenceRepository() {
        this.client = SupabaseClient.getInstance();
    }

    /**
     * Mark member as online (app opened)
     * @return session token for this presence session
     */
    public String markMemberOnline(int memberId, String deviceInfo) {
        try {
            // Generate unique session token
            String sessionToken = UUID.randomUUID().toString();

            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("session_token", sessionToken);
            data.put("is_online", true);
            data.put("device_info", deviceInfo);
            data.put("last_heartbeat", getCurrentTimestamp());
            data.put("app_opened_at", getCurrentTimestamp());

            JSONArray result = client.insert(TABLE_NAME, data);

            if (result.length() > 0) {
                Log.d(TAG, "✅ Member " + memberId + " marked online");
                return sessionToken;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking member online: " + e.getMessage());
        }
        return null;
    }

    /**
     * Send heartbeat to keep member marked as online
     */
    public boolean sendHeartbeat(int memberId, String sessionToken) {
        try {
            String filter = "member_id=eq." + memberId + "&session_token=eq." + sessionToken;

            JSONObject data = new JSONObject();
            data.put("last_heartbeat", getCurrentTimestamp());
            data.put("is_online", true);

            JSONArray result = client.update(TABLE_NAME, filter, data);
            return result.length() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error sending heartbeat: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark member as offline (app closed)
     */
    public boolean markMemberOffline(int memberId, String sessionToken) {
        try {
            String filter = "member_id=eq." + memberId + "&session_token=eq." + sessionToken;

            JSONObject data = new JSONObject();
            data.put("is_online", false);

            JSONArray result = client.update(TABLE_NAME, filter, data);

            if (result.length() > 0) {
                Log.d(TAG, "✅ Member " + memberId + " marked offline");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking member offline: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get all currently online members
     */
    public List<Map<String, Object>> getOnlineMembers() {
        List<Map<String, Object>> onlineMembers = new ArrayList<>();
        try {
            // Get members who are online and had heartbeat in last 30 seconds
            String filter = "is_online=eq.true&order=last_heartbeat.desc";
            JSONArray result = client.select(TABLE_NAME, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);

                // Check if heartbeat is recent (within 30 seconds)
                String lastHeartbeat = obj.optString("last_heartbeat");
                if (isRecentHeartbeat(lastHeartbeat)) {
                    Map<String, Object> presence = new HashMap<>();
                    presence.put("id", obj.optInt("id"));
                    presence.put("member_id", obj.optInt("member_id"));
                    presence.put("session_token", obj.optString("session_token"));
                    presence.put("last_heartbeat", lastHeartbeat);
                    presence.put("app_opened_at", obj.optString("app_opened_at"));
                    presence.put("device_info", obj.optString("device_info"));

                    onlineMembers.add(presence);
                }
            }

            Log.d(TAG, "📊 Found " + onlineMembers.size() + " online members");
        } catch (Exception e) {
            Log.e(TAG, "Error getting online members: " + e.getMessage());
        }
        return onlineMembers;
    }

    /**
     * Get online members with their full details
     */
    public List<Map<String, Object>> getOnlineMembersWithDetails() {
        List<Map<String, Object>> onlineMembers = new ArrayList<>();
        try {
            List<Map<String, Object>> presenceData = getOnlineMembers();

            MemberRepository memberRepo = new MemberRepository();

            for (Map<String, Object> presence : presenceData) {
                int memberId = (int) presence.get("member_id");

                // Get member details
                com.maxfit.vipgymapp.Model.Member member = memberRepo.getMemberById(memberId);

                if (member != null) {
                    Map<String, Object> memberWithPresence = new HashMap<>();
                    memberWithPresence.put("member_id", memberId);
                    memberWithPresence.put("first_name", member.getFirstName());
                    memberWithPresence.put("last_name", member.getLastName());
                    memberWithPresence.put("membership_id", member.getMembershipId());
                    memberWithPresence.put("last_heartbeat", presence.get("last_heartbeat"));
                    memberWithPresence.put("app_opened_at", presence.get("app_opened_at"));
                    memberWithPresence.put("device_info", presence.get("device_info"));

                    onlineMembers.add(memberWithPresence);
                }
            }

            Log.d(TAG, "📊 " + onlineMembers.size() + " members currently online");
        } catch (Exception e) {
            Log.e(TAG, "Error getting online members with details: " + e.getMessage());
        }
        return onlineMembers;
    }

    /**
     * Get count of currently online members
     */
    public int getOnlineMemberCount() {
        return getOnlineMembers().size();
    }

    /**
     * Check if a specific member is currently online
     */
    public boolean isMemberOnline(int memberId) {
        try {
            String filter = "member_id=eq." + memberId + "&is_online=eq.true";
            JSONArray result = client.select(TABLE_NAME, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                String lastHeartbeat = obj.optString("last_heartbeat");
                if (isRecentHeartbeat(lastHeartbeat)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if member online: " + e.getMessage());
        }
        return false;
    }

    /**
     * Clean up old presence records for a member
     */
    public void cleanupOldSessions(int memberId) {
        try {
            String filter = "member_id=eq." + memberId + "&is_online=eq.false";
            client.delete(TABLE_NAME, filter);
            Log.d(TAG, "🧹 Cleaned up old sessions for member " + memberId);
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up old sessions: " + e.getMessage());
        }
    }

    /**
     * Helper: Get current timestamp in ISO format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Helper: Check if heartbeat timestamp is within last 30 seconds
     */
    private boolean isRecentHeartbeat(String timestampStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date heartbeatTime = sdf.parse(timestampStr.replace("Z", "").split("\\.")[0]);
            Date now = new Date();

            long diffInMillis = now.getTime() - heartbeatTime.getTime();
            long diffInSeconds = diffInMillis / 1000;

            return diffInSeconds <= 30;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing heartbeat timestamp: " + e.getMessage());
            return false;
        }
    }
}