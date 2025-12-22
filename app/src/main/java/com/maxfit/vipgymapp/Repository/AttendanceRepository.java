package com.maxfit.vipgymapp.Repository;

import android.util.Log;

import com.maxfit.vipgymapp.Config.SupabaseConfig;
import com.maxfit.vipgymapp.Network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private SupabaseClient client;

    public AttendanceRepository() {
        this.client = SupabaseClient.getInstance();
    }

    // Check in member (start attendance)
    public int checkIn(int memberId) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            // start_time will use DEFAULT CURRENT_TIMESTAMP

            JSONArray result = client.insert(SupabaseConfig.TABLE_ATTENDANCE, data);

            if (result.length() > 0) {
                return result.getJSONObject(0).getInt("id");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking in: " + e.getMessage());
        }
        return -1;
    }

    // Check out member (end attendance)
    public boolean checkOut(int attendanceId) {
        try {
            String filter = "id=eq." + attendanceId;
            JSONObject data = new JSONObject();
            data.put("end_time", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));

            client.update(SupabaseConfig.TABLE_ATTENDANCE, filter, data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking out: " + e.getMessage());
            return false;
        }
    }

    // Get active attendance for member (not checked out)
    public int getActiveAttendance(int memberId) {
        try {
            String filter = "member_id=eq." + memberId + "&end_time=is.null";
            JSONArray result = client.select(SupabaseConfig.TABLE_ATTENDANCE, filter);

            if (result.length() > 0) {
                return result.getJSONObject(0).getInt("id");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting active attendance: " + e.getMessage());
        }
        return -1;
    }

    // Get attendance history for member
    public List<Map<String, Object>> getAttendanceHistory(int memberId, int limit) {
        List<Map<String, Object>> attendanceList = new ArrayList<>();
        try {
            String filter = "member_id=eq." + memberId + "&order=start_time.desc&limit=" + limit;
            JSONArray result = client.select(SupabaseConfig.TABLE_ATTENDANCE, filter);

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                Map<String, Object> attendance = new HashMap<>();
                attendance.put("id", obj.optInt("id"));
                attendance.put("start_time", obj.optString("start_time"));
                attendance.put("end_time", obj.optString("end_time"));
                attendanceList.add(attendance);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting attendance history: " + e.getMessage());
        }
        return attendanceList;
    }

    // Get attendance count for current month
    public int getMonthlyAttendanceCount(int memberId) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            String startOfMonth = sdf.format(cal.getTime());

            String filter = "member_id=eq." + memberId + "&start_time=gte." + startOfMonth;
            JSONArray result = client.select(SupabaseConfig.TABLE_ATTENDANCE, filter);

            return result.length();
        } catch (Exception e) {
            Log.e(TAG, "Error getting monthly attendance: " + e.getMessage());
        }
        return 0;
    }

    // Get attendance dates for calendar view
    public List<String> getAttendanceDates(int memberId, int months) {
        List<String> dates = new ArrayList<>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -months);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            String startDate = sdf.format(cal.getTime());

            String filter = "member_id=eq." + memberId + "&start_time=gte." + startDate;
            JSONArray result = client.select(SupabaseConfig.TABLE_ATTENDANCE, filter);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (int i = 0; i < result.length(); i++) {
                JSONObject obj = result.getJSONObject(i);
                String startTime = obj.optString("start_time");
                if (!startTime.isEmpty()) {
                    // Extract date part
                    String date = startTime.split("T")[0];
                    if (!dates.contains(date)) {
                        dates.add(date);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting attendance dates: " + e.getMessage());
        }
        return dates;
    }

    // Check if member attended today
    public boolean hasAttendedToday(int memberId) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            List<String> dates = getAttendanceDates(memberId, 1);
            return dates.contains(today);
        } catch (Exception e) {
            Log.e(TAG, "Error checking today's attendance: " + e.getMessage());
        }
        return false;
    }
}