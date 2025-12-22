package com.maxfit.vipgymapp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.maxfit.vipgymapp.Model.Member;
import com.google.gson.Gson;

public class SessionManager {
    private static final String PREF_NAME = "MaxFitSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_MEMBER_ID = "memberId";
    private static final String KEY_MEMBER_DATA = "memberData";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_MEMBERSHIP_ID = "membershipId";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;
    private Gson gson;

    public SessionManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
        this.gson = new Gson();
    }

    // Create login session
    public void createLoginSession(Member member) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_MEMBER_ID, member.getId());
        editor.putString(KEY_PHONE_NUMBER, member.getPhoneNumber());
        editor.putString(KEY_MEMBERSHIP_ID, member.getMembershipId());
        editor.putString(KEY_MEMBER_DATA, gson.toJson(member));
        editor.apply();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get current member ID
    public int getMemberId() {
        return prefs.getInt(KEY_MEMBER_ID, -1);
    }

    // Get current member data
    public Member getMemberData() {
        String json = prefs.getString(KEY_MEMBER_DATA, null);
        if (json != null) {
            return gson.fromJson(json, Member.class);
        }
        return null;
    }

    // Get phone number
    public String getPhoneNumber() {
        return prefs.getString(KEY_PHONE_NUMBER, "");
    }

    // Get membership ID
    public String getMembershipId() {
        return prefs.getString(KEY_MEMBERSHIP_ID, "");
    }

    // Update member data
    public void updateMemberData(Member member) {
        editor.putString(KEY_MEMBER_DATA, gson.toJson(member));
        editor.putString(KEY_PHONE_NUMBER, member.getPhoneNumber());
        editor.putString(KEY_MEMBERSHIP_ID, member.getMembershipId());
        editor.apply();
    }

    // Logout user
    public void logout() {
        editor.clear();
        editor.apply();
    }

    // Check login and redirect
    public void checkLogin(Context context) {
        if (!this.isLoggedIn()) {
            // Redirect to login
            context.startActivity(new android.content.Intent(context,
                    com.maxfit.vipgymapp.Activity.PhoneNumberActivity.class));
        }
    }
}