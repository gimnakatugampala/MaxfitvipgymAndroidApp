package com.maxfit.vipgymapp.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.maxfit.vipgymapp.Activity.GetStartedActivity;
import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.MemberRepository;
import com.maxfit.vipgymapp.Repository.WorkoutCompletionRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private SessionManager sessionManager;
    private MemberRepository memberRepository;
    private WorkoutCompletionRepository workoutCompletionRepository;
    private ExecutorService executorService;
    private int memberId;

    // UI Elements
    private TextView userName;
    private TextView userLocation;
    private TextView joinDate;
    private GridLayout statsGrid;
    private GridLayout trainingLogGrid;
    private LinearLayout monthLabelsContainer; // ✅ New Container for Labels
    private LineChart momentumChart;
    private HorizontalScrollView trainingScrollView;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize helpers
        sessionManager = new SessionManager(getContext());
        memberRepository = new MemberRepository();
        workoutCompletionRepository = new WorkoutCompletionRepository();
        executorService = Executors.newSingleThreadExecutor();
        memberId = sessionManager.getMemberId();

        // Get view references
        userName = view.findViewById(R.id.user_name);
        userLocation = view.findViewById(R.id.user_location);
        joinDate = view.findViewById(R.id.join_date);
        statsGrid = view.findViewById(R.id.profile_stats_grid);

        trainingLogGrid = view.findViewById(R.id.training_log_grid);
        monthLabelsContainer = view.findViewById(R.id.month_labels_container); // ✅ Bind View
        trainingScrollView = view.findViewById(R.id.training_scroll_view);

        momentumChart = view.findViewById(R.id.momentum_chart);

        // Load static user data
        loadUserData();

        // Load stats from DB
        loadStatistics();

        // Add Logout Button
        addLogoutButton(view);

        return view;
    }

    private void loadUserData() {
        Member member = sessionManager.getMemberData();
        if (member != null) {
            if (userName != null) userName.setText(member.getFullName());
            if (userLocation != null) {
                String membershipId = member.getMembershipId();
                if (membershipId != null && !membershipId.isEmpty()) {
                    userLocation.setText("📋 ID: " + membershipId);
                } else {
                    userLocation.setText("📞 " + member.getPhoneNumber());
                }
            }
            if (joinDate != null && member.getCreatedDate() != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(member.getCreatedDate());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                    joinDate.setText("🕒 Joined " + outputFormat.format(date));
                } catch (Exception e) {
                    joinDate.setText("🕒 Member");
                }
            }
        }
    }

    private void loadStatistics() {
        if (memberId == -1) return;

        executorService.execute(() -> {
            try {
                // 1. Fetch Stats for Grid
                Map<String, Object> workoutStats = workoutCompletionRepository.getWorkoutStats(memberId);
                int totalSessions = (int) workoutStats.getOrDefault("total_workouts", 0);

                int calculatedWeeks = 0;
                Member member = sessionManager.getMemberData();
                if (member != null && member.getCreatedDate() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date joined = sdf.parse(member.getCreatedDate());
                        Date now = new Date();
                        long diff = now.getTime() - joined.getTime();
                        calculatedWeeks = (int) (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 7);
                        if (calculatedWeeks < 1) calculatedWeeks = 1;
                    } catch (Exception e) {}
                }
                final int totalWeeks = calculatedWeeks;
                int totalCycles = memberRepository.getTotalCycles(memberId);
                int uniqueExercises = memberRepository.getUniqueExercisesCount(memberId);

                // 2. Fetch Completion Data
                List<Map<String, Object>> allCompletions = workoutCompletionRepository.getAllCompletions(memberId);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    updateStatsGrid(totalSessions, totalWeeks, totalCycles, uniqueExercises);
                    setupTrainingLog(allCompletions);
                    setupMomentumChart(allCompletions);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics", e);
            }
        });
    }

    private void updateStatsGrid(int sessions, int weeks, int cycles, int exercises) {
        if (statsGrid == null) return;
        if (statsGrid.getChildCount() >= 4) {
            setStatValue(statsGrid.getChildAt(0), String.valueOf(sessions));
            setStatValue(statsGrid.getChildAt(1), String.valueOf(weeks));
            setStatValue(statsGrid.getChildAt(2), String.valueOf(cycles));
            setStatValue(statsGrid.getChildAt(3), String.valueOf(exercises));
        }
    }

    private void setStatValue(View container, String value) {
        if (container instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) container;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if (tv.getTextSize() > 40) {
                        tv.setText(value);
                        return;
                    }
                }
            }
            if (group.getChildCount() > 0 && group.getChildAt(0) instanceof TextView) {
                ((TextView) group.getChildAt(0)).setText(value);
            }
        }
    }

    private void setupTrainingLog(List<Map<String, Object>> completions) {
        if (trainingLogGrid == null || monthLabelsContainer == null || getContext() == null) return;

        trainingLogGrid.removeAllViews();
        monthLabelsContainer.removeAllViews(); // ✅ Clear old labels

        // --- Configuration ---
        // Using slightly larger squares for better visibility
        int squareSize = dpToPx(12);
        int margin = dpToPx(1);
        int columnWidth = squareSize + (2 * margin); // Exact width of one column

        int columns = 53; // 1 year approx
        int rows = 7;
        int totalDays = columns * rows;

        // Setup Grid
        trainingLogGrid.setOrientation(GridLayout.VERTICAL);
        trainingLogGrid.setColumnCount(columns);
        trainingLogGrid.setRowCount(rows);

        // Prepare Data Map
        Map<String, Integer> heatMap = new HashMap<>();
        for (Map<String, Object> c : completions) {
            String date = (String) c.get("date");
            int count = (int) c.get("count");
            heatMap.put(date, Math.min(count, 3));
        }

        // Start Date (1 year ago)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -totalDays + 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        // --- 1. Build Grid Squares ---
        Calendar gridCal = (Calendar) calendar.clone();
        for (int i = 0; i < totalDays; i++) {
            String dateKey = sdf.format(gridCal.getTime());
            int intensity = heatMap.getOrDefault(dateKey, 0);

            View square = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = squareSize;
            params.height = squareSize;
            params.setMargins(margin, margin, margin, margin);

            square.setLayoutParams(params);
            square.setBackgroundColor(getColorForIntensity(intensity));

            trainingLogGrid.addView(square);
            gridCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // --- 2. Build Month Labels ---
        Calendar labelCal = (Calendar) calendar.clone();
        String currentMonth = "";

        for (int col = 0; col < columns; col++) {
            // Get month name of the first day of this column
            String month = monthFormat.format(labelCal.getTime());

            TextView label = new TextView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    columnWidth, // ✅ Width matches grid column exactly
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            label.setLayoutParams(params);
            label.setTextSize(10);
            label.setTextColor(Color.GRAY);
            label.setGravity(Gravity.CENTER_HORIZONTAL);

            // Only show label if month changed
            if (!month.equals(currentMonth)) {
                label.setText(month);
                currentMonth = month;
            } else {
                label.setText(""); // Placeholder to keep alignment
            }

            monthLabelsContainer.addView(label);

            // Move to next week
            labelCal.add(Calendar.DAY_OF_YEAR, 7);
        }

        // Auto-scroll to end
        if (trainingScrollView != null) {
            trainingScrollView.post(() -> trainingScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
        }
    }

    private void setupMomentumChart(List<Map<String, Object>> completions) {
        if (momentumChart == null) return;

        Map<String, Integer> monthlyData = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        List<String> lastMonths = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            lastMonths.add(monthFormat.format(cal.getTime()));
            cal.add(Calendar.MONTH, -1);
        }
        Collections.reverse(lastMonths);

        for (Map<String, Object> c : completions) {
            try {
                Date date = parseFormat.parse((String) c.get("date"));
                String month = monthFormat.format(date);
                int count = (int) c.get("count");
                if (lastMonths.contains(month)) {
                    monthlyData.put(month, monthlyData.getOrDefault(month, 0) + count);
                }
            } catch (Exception e) {}
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < lastMonths.size(); i++) {
            String month = lastMonths.get(i);
            entries.add(new Entry(i, monthlyData.getOrDefault(month, 0)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Monthly Workouts");
        dataSet.setColor(Color.YELLOW);
        dataSet.setCircleColor(Color.YELLOW);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, 255, 211, 0));
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        momentumChart.setData(lineData);
        momentumChart.getDescription().setEnabled(false);
        momentumChart.getLegend().setEnabled(false);
        momentumChart.getAxisRight().setEnabled(false);

        XAxis xAxis = momentumChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(lastMonths));

        YAxis yAxis = momentumChart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setAxisMinimum(0);

        momentumChart.invalidate();
    }

    private void addLogoutButton(View view) {
        // Safe check for parent container
        if (view instanceof ViewGroup && ((ViewGroup) view).getChildCount() > 0) {
            View child = ((ViewGroup) view).getChildAt(0);
            if (child instanceof ViewGroup) {
                ViewGroup parentLayout = (ViewGroup) child;

                Button logoutButton = new Button(getContext());
                logoutButton.setText("LOGOUT");
                logoutButton.setBackgroundColor(Color.parseColor("#FF5252"));
                logoutButton.setTextColor(Color.WHITE);
                logoutButton.setAllCaps(true);

                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(40, 60, 40, 60);
                logoutButton.setLayoutParams(params);

                logoutButton.setOnClickListener(v -> showLogoutConfirmation());
                parentLayout.addView(logoutButton);
            }
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(getActivity(), GetStartedActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int getColorForIntensity(int value) {
        switch (value) {
            case 0: return Color.parseColor("#2C2C2C"); // Empty
            case 1: return Color.parseColor("#fff8d8"); // Low
            case 2: return Color.parseColor("#ffd300"); // Medium
            default: return Color.parseColor("#b39400"); // High
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}