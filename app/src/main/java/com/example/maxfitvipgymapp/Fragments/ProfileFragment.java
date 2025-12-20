package com.example.maxfitvipgymapp.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.maxfitvipgymapp.Activity.GetStartedActivity;
import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView userName;
    private TextView userLocation;
    private TextView joinDate;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // ✅ Initialize SessionManager
        sessionManager = new SessionManager(getContext());

        // ✅ Get view references
        userName = view.findViewById(R.id.user_name);
        userLocation = view.findViewById(R.id.user_location);
        joinDate = view.findViewById(R.id.join_date);

        // ✅ Load user data
        loadUserData();

        // ✅ Add Logout Button
        addLogoutButton(view);

        // Setup training log grid
        GridLayout trainingLogGrid = view.findViewById(R.id.training_log_grid);

        int totalDays = 364;  // 52 weeks * 7 days
        int columns = 52;
        int rows = 7;

        trainingLogGrid.setColumnCount(columns);
        trainingLogGrid.setRowCount(rows);

        // Mock training data
        int[] trainingData = new int[totalDays];
        for (int i = 0; i < totalDays; i++) {
            trainingData[i] = (int) (Math.random() * 4);  // Random 0–3
        }

        for (int i = 0; i < totalDays; i++) {
            View square = new View(getContext());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(10);
            params.height = dpToPx(10);
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            square.setLayoutParams(params);

            square.setBackgroundColor(getColor(trainingData[i]));
            trainingLogGrid.addView(square);
        }

        // Setup momentum chart
        LineChart chart = view.findViewById(R.id.momentum_chart);

        int[] monthData = {75, 77, 92, 52, 4, 91, 87, 49, 51, 4, 26, 91};  // Mock data
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < monthData.length; i++) {
            entries.add(new Entry(i, monthData[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(Color.YELLOW);
        dataSet.setCircleColor(Color.YELLOW);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, 255, 211, 0));
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.getAxisRight().setEnabled(false);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(Color.GRAY);
        yAxis.setAxisMinimum(0);
        yAxis.setAxisMaximum(100);
        yAxis.setGranularity(10f);
        yAxis.setDrawGridLines(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setGranularity(1f);

        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < months.length) {
                    return months[index];
                } else {
                    return "";
                }
            }
        });

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.getLegend().setEnabled(false);

        chart.invalidate(); // Refresh chart

        return view;
    }

    // ✅ NEW METHOD: Load user data from session
    private void loadUserData() {
        Member member = sessionManager.getMemberData();
        if (member != null) {
            // Set user name
            if (userName != null) {
                userName.setText(member.getFullName());
            }

            // Set membership ID or phone
            if (userLocation != null) {
                String membershipId = member.getMembershipId();
                if (membershipId != null && !membershipId.isEmpty()) {
                    userLocation.setText("📋 Member ID: " + membershipId);
                } else {
                    userLocation.setText("📞 " + member.getPhoneNumber());
                }
            }

            // Set join date
            if (joinDate != null && member.getCreatedDate() != null) {
                String createdDate = member.getCreatedDate();
                if (!createdDate.isEmpty()) {
                    // Format: "2024-12-20T10:30:00+00:00" -> "Joined Dec 20"
                    try {
                        String[] parts = createdDate.split("T")[0].split("-");
                        if (parts.length >= 3) {
                            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                            int month = Integer.parseInt(parts[1]) - 1;
                            String day = parts[2];
                            joinDate.setText("🕒 Joined " + day + " " + months[month]);
                        }
                    } catch (Exception e) {
                        joinDate.setText("🕒 Member");
                    }
                } else {
                    joinDate.setText("🕒 Member");
                }
            }
        }
    }

    // ✅ NEW METHOD: Add logout button to profile
    private void addLogoutButton(View view) {
        // Find the parent layout (you may need to adjust this based on your layout)
        ViewGroup parentLayout = view.findViewById(R.id.profile_stats_grid);
        if (parentLayout != null && parentLayout.getParent() instanceof ViewGroup) {
            ViewGroup mainLayout = (ViewGroup) parentLayout.getParent();

            // Create logout button
            Button logoutButton = new Button(getContext());
            logoutButton.setText("LOGOUT");
            logoutButton.setBackgroundColor(Color.parseColor("#FF5252"));
            logoutButton.setTextColor(Color.WHITE);
            logoutButton.setAllCaps(true);
            logoutButton.setTextSize(16);
            logoutButton.setPadding(32, 24, 32, 24);

            // Set layout params
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(32, 32, 32, 64);
            logoutButton.setLayoutParams(params);

            // Add click listener
            logoutButton.setOnClickListener(v -> showLogoutConfirmation());

            // Add button to layout
            mainLayout.addView(logoutButton);
        }
    }

    // ✅ NEW METHOD: Show logout confirmation dialog
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear session
                    sessionManager.logout();

                    // Redirect to login screen
                    Intent intent = new Intent(getActivity(), GetStartedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Convert dp to pixels
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Color schema matching the one you provided
    private int getColor(int value) {
        switch (value) {
            case 0:
                return Color.parseColor("#333333");   // Dark background
            case 1:
                return Color.parseColor("#fff8d8");   // Light yellow
            case 2:
                return Color.parseColor("#ffd300");   // Bright yellow
            case 3:
                return Color.parseColor("#b39400");   // Darker gold
            default:
                return Color.parseColor("#000000");   // Fallback black
        }
    }
}