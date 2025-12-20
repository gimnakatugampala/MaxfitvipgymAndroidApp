package com.example.maxfitvipgymapp.Repository;

import android.util.Log;

import com.example.maxfitvipgymapp.Config.SupabaseConfig;
import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MemberRepository {
    private static final String TAG = "MemberRepository";
    private SupabaseClient client;

    public MemberRepository() {
        this.client = SupabaseClient.getInstance();
    }

    // Get member by phone number
    public Member getMemberByPhone(String phoneNumber) {
        try {
            String filter = "phone_number=eq." + phoneNumber;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);

            if (result.length() > 0) {
                return new Member(result.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting member by phone: " + e.getMessage());
        }
        return null;
    }

    // Get member by membership ID
    public Member getMemberByMembershipId(String membershipId) {
        try {
            String filter = "membership_id=eq." + membershipId;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);

            if (result.length() > 0) {
                return new Member(result.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting member by membership ID: " + e.getMessage());
        }
        return null;
    }

    // Get member by ID
    public Member getMemberById(int id) {
        try {
            String filter = "id=eq." + id;
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);

            if (result.length() > 0) {
                return new Member(result.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting member by ID: " + e.getMessage());
        }
        return null;
    }

    // ✅ FIXED: Generate next member code
    private String generateNextMemberCode() {
        try {
            // Get all members ordered by id descending to find the highest code
            String filter = "order=id.desc&limit=100"; // Get last 100 to ensure we find highest code
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);

            int maxNumber = 0; // Start from 0, will increment to 1 for first member

            // Iterate through all results to find the highest code number
            for (int i = 0; i < result.length(); i++) {
                String code = result.getJSONObject(i).optString("code", "");

                // Extract number from code like "M_001"
                if (code != null && code.startsWith("M_")) {
                    try {
                        String numberPart = code.substring(2); // Remove "M_"
                        int codeNumber = Integer.parseInt(numberPart);
                        if (codeNumber > maxNumber) {
                            maxNumber = codeNumber;
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Could not parse code number from: " + code);
                    }
                }
            }

            // Increment to get next number
            int nextNumber = maxNumber + 1;

            // Format as M_001, M_002, etc.
            String generatedCode = String.format("M_%03d", nextNumber);
            Log.d(TAG, "Generated member code: " + generatedCode + " (previous max: " + maxNumber + ")");

            return generatedCode;

        } catch (Exception e) {
            Log.e(TAG, "Error generating member code: " + e.getMessage());
            e.printStackTrace();
            // Return a timestamp-based code as fallback
            return "M_" + System.currentTimeMillis();
        }
    }

    // ✅ UPDATED: Create new member with auto-generated code
    public Member createMember(Member member) {
        try {
            // Generate code if not already set
            if (member.getCode() == null || member.getCode().isEmpty()) {
                String generatedCode = generateNextMemberCode();
                member.setCode(generatedCode);
                Log.d(TAG, "Generated member code: " + generatedCode);
            }

            JSONObject data = member.toJSON();

            // ✅ Add the generated code to the JSON
            data.put("code", member.getCode());

            JSONArray result = client.insert(SupabaseConfig.TABLE_MEMBERS, data);

            if (result.length() > 0) {
                return new Member(result.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating member: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Update member
    public Member updateMember(int memberId, Member member) {
        try {
            String filter = "id=eq." + memberId;
            JSONObject data = member.toJSON();
            JSONArray result = client.update(SupabaseConfig.TABLE_MEMBERS, filter, data);

            if (result.length() > 0) {
                return new Member(result.getJSONObject(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating member: " + e.getMessage());
        }
        return null;
    }

    // Update last active
    public void updateLastActive(int memberId) {
        try {
            String filter = "id=eq." + memberId;
            JSONObject data = new JSONObject();
            data.put("last_active", "now()");
            client.update(SupabaseConfig.TABLE_MEMBERS, filter, data);
        } catch (Exception e) {
            Log.e(TAG, "Error updating last active: " + e.getMessage());
        }
    }

    // Check if member exists
    public boolean memberExists(String phoneNumber) {
        return getMemberByPhone(phoneNumber) != null;
    }

    // Get all active members
    public List<Member> getAllActiveMembers() {
        List<Member> members = new ArrayList<>();
        try {
            String filter = "is_active=eq.true&is_deleted=eq.false";
            JSONArray result = client.select(SupabaseConfig.TABLE_MEMBERS, filter);

            for (int i = 0; i < result.length(); i++) {
                members.add(new Member(result.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting active members: " + e.getMessage());
        }
        return members;
    }
}