package com.example.maxfitvipgymapp.Repository;

import android.util.Log;

import com.example.maxfitvipgymapp.Config.SupabaseConfig;
import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemberRepository {
    private static final String TAG = "MemberRepository";
    private SupabaseClient client;

    public MemberRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public Member getMemberByPhone(String phoneNumber) {
        try {
            String filter = "phone_number=eq." + phoneNumber;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);
            if (result.length() > 0) return new Member(result.getJSONObject(0));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return null;
    }

    public Member getMemberByMembershipId(String membershipId) {
        try {
            String filter = "membership_id=eq." + membershipId;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);
            if (result.length() > 0) return new Member(result.getJSONObject(0));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return null;
    }

    public Member getMemberById(int id) {
        try {
            String filter = "id=eq." + id;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);
            if (result.length() > 0) return new Member(result.getJSONObject(0));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return null;
    }

    private String generateNextMemberCode() {
        try {
            String filter = "order=id.desc&limit=100";
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);
            int maxNumber = 0;
            for (int i = 0; i < result.length(); i++) {
                String code = result.getJSONObject(i).optString("code", "");
                if (code != null && code.startsWith("M_")) {
                    try {
                        int codeNumber = Integer.parseInt(code.substring(2));
                        if (codeNumber > maxNumber) maxNumber = codeNumber;
                    } catch (NumberFormatException e) { }
                }
            }
            return String.format("M_%03d", maxNumber + 1);
        } catch (Exception e) {
            return "M_" + System.currentTimeMillis();
        }
    }

    public Member createMember(Member member) {
        try {
            if (member.getCode() == null || member.getCode().isEmpty()) {
                member.setCode(generateNextMemberCode());
            }
            JSONObject data = member.toJSON();
            data.put("code", member.getCode());
            JSONArray result = client.insert(SupabaseConfig.TABLE_MEMBERS, data);
            if (result.length() > 0) return new Member(result.getJSONObject(0));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return null;
    }

    public Member updateMember(int memberId, Member member) {
        try {
            String filter = "id=eq." + memberId;
            JSONArray result = client.update(SupabaseConfig.TABLE_MEMBERS, filter, member.toJSON());
            if (result.length() > 0) return new Member(result.getJSONObject(0));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return null;
    }

    public void updateLastActive(int memberId) {
        try {
            String filter = "id=eq." + memberId;
            JSONObject data = new JSONObject();
            data.put("last_active", "now()");
            client.update(SupabaseConfig.TABLE_MEMBERS, filter, data);
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
    }

    public boolean memberExists(String phoneNumber) {
        return getMemberByPhone(phoneNumber) != null;
    }

    public List<Member> getAllActiveMembers() {
        List<Member> members = new ArrayList<>();
        try {
            String filter = "is_active=eq.true&is_deleted=eq.false";
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);
            for (int i = 0; i < result.length(); i++) members.add(new Member(result.getJSONObject(i)));
        } catch (Exception e) { Log.e(TAG, "Error: " + e.getMessage()); }
        return members;
    }

    // ✅ NEW: Get Total Cycles (Member Schedules)
    public int getTotalCycles(int memberId) {
        try {
            String filter = "member_id=eq." + memberId;
            // Supabase client wrapper returns JSONArray, we count length
            JSONArray result = client.select("member_workout_schedule", filter);
            return result.length();
        } catch (Exception e) {
            Log.e(TAG, "Error getting total cycles: " + e.getMessage());
            return 0;
        }
    }

    // ✅ NEW: Get Unique Exercises count
    public int getUniqueExercisesCount(int memberId) {
        try {
            // 1. Get all schedule IDs for member
            String scheduleFilter = "member_id=eq." + memberId;
            JSONArray schedules = client.select("member_workout_schedule", scheduleFilter);

            if (schedules.length() == 0) return 0;

            Set<Integer> uniqueWorkoutIds = new HashSet<>();

            // 2. Loop through schedules to get details
            // (Not efficient for large data, but works with current simple client)
            for (int i = 0; i < schedules.length(); i++) {
                int memberScheduleId = schedules.getJSONObject(i).optInt("id");

                String detailsFilter = "member_schedule_id=eq." + memberScheduleId;
                JSONArray details = client.select("member_workout_schedule_details", detailsFilter);

                for (int j = 0; j < details.length(); j++) {
                    int workoutId = details.getJSONObject(j).optInt("workout_id");
                    if (workoutId > 0) {
                        uniqueWorkoutIds.add(workoutId);
                    }
                }
            }
            return uniqueWorkoutIds.size();
        } catch (Exception e) {
            Log.e(TAG, "Error counting unique exercises: " + e.getMessage());
            return 0;
        }
    }
}