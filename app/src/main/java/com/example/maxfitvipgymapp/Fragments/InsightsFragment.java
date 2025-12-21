package com.example.maxfitvipgymapp.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.gridlayout.widget.GridLayout;

import com.example.maxfitvipgymapp.Adapter.DateAdapter;
import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Repository.ProgressRepository;
import com.example.maxfitvipgymapp.Repository.WorkoutCompletionRepository;
import com.example.maxfitvipgymapp.Utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InsightsFragment extends Fragment {

    private static final String TAG = "InsightsFragment";

    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;
    private LineChart weightChart, bicepChart, hipChart, chestChart;
    private Button btnWeekly, btnMonthly, btnYearly;
    private ProgressRepository progressRepository;
    private WorkoutCompletionRepository workoutCompletionRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private int memberId;

    // UI Elements
    private View healthGradeCard;

    // Current period selection
    private String currentPeriod = "Weekly";
    private int currentDays = 7;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        // Initialize repositories
        sessionManager = new SessionManager(getContext());
        progressRepository = new ProgressRepository();
        workoutCompletionRepository = new WorkoutCompletionRepository();
        executorService = Executors.newSingleThreadExecutor();
        memberId = sessionManager.getMemberId();

        Log.d(TAG, "Initializing InsightsFragment for member ID: " + memberId);

        if (memberId == -1) {
            Toast.makeText(getContext(), "Error: Member not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize views
        initializeViews(view);

        // Setup date selector
        setupDateSelector();

        // Load initial data
        loadAllData();

        return view;
    }

    private void initializeViews(View view) {
        // Charts
        weightChart = view.findViewById(R.id.weight_chart);
        bicepChart = view.findViewById(R.id.bicep_chart);
        hipChart = view.findViewById(R.id.hip_chart);
        chestChart = view.findViewById(R.id.chest_chart);

        // Buttons
        btnWeekly = view.findViewById(R.id.btn_weekly);
        btnMonthly = view.findViewById(R.id.btn_monthly);
        btnYearly = view.findViewById(R.id.btn_yearly);

        // Date selector
        dateRecyclerView = view.findViewById(R.id.date_recycler_view);
        dateRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        // Health grade card
        healthGradeCard = view.findViewById(R.id.health_grade_card);

        // Button listeners
        btnWeekly.setOnClickListener(v -> {
            updateButtonStates(btnWeekly);
            loadChartData("Weekly", 7);
        });

        btnMonthly.setOnClickListener(v -> {
            updateButtonStates(btnMonthly);
            loadChartData("Monthly", 30);
        });

        btnYearly.setOnClickListener(v -> {
            updateButtonStates(btnYearly);
            loadChartData("Yearly", 365);
        });
    }

    private void updateButtonStates(Button selectedButton) {
        // Reset all buttons
        btnWeekly.setBackgroundTintList(getResources().getColorStateList(android.R.color.transparent));
        btnMonthly.setBackgroundTintList(getResources().getColorStateList(android.R.color.transparent));
        btnYearly.setBackgroundTintList(getResources().getColorStateList(android.R.color.transparent));

        btnWeekly.setTextColor(Color.parseColor("#AAAAAA"));
        btnMonthly.setTextColor(Color.parseColor("#AAAAAA"));
        btnYearly.setTextColor(Color.parseColor("#AAAAAA"));

        // Highlight selected
        selectedButton.setBackgroundTintList(getResources().getColorStateList(R.color.yellow));
        selectedButton.setTextColor(Color.parseColor("#000000"));
    }

    private void loadAllData() {
        if (!isAdded()) return;

        Log.d(TAG, "Loading all data for member: " + memberId);

        // Load stats grid
        loadLatestStats();

        // Load health grade
        calculateHealthGrade();

        // Load charts with default period
        loadChartData(currentPeriod, currentDays);

        // Load workout statistics
        loadWorkoutStats();
    }

    private void setupDateSelector() {
        List<DateModel> dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());
            boolean isSelected = (i == 0);
            dateList.add(new DateModel(day, date, isSelected, false));
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        Collections.reverse(dateList);
        int selectedIndex = dateList.size() - 1;

        dateAdapter = new DateAdapter(dateList, position -> {
            dateAdapter.setSelected(position);
            // You can load specific date data here if needed
        });
        dateRecyclerView.setAdapter(dateAdapter);
        dateAdapter.setSelected(selectedIndex);
        dateRecyclerView.scrollToPosition(selectedIndex);
    }

    private void calculateHealthGrade() {
        executorService.execute(() -> {
            try {
                // 1. Get Consistency Data (Last 30 Days)
                List<String> recentDates = workoutCompletionRepository.getCompletionDates(memberId, 1); // 1 Month
                int workoutsLast30Days = recentDates.size();

                // 2. Get Streak & Effort Data
                Map<String, Object> stats = workoutCompletionRepository.getWorkoutStats(memberId);
                int currentStreak = 0;
                int totalDuration = 0;
                int totalWorkouts = 0;

                if (stats != null) {
                    if (stats.containsKey("current_streak")) currentStreak = (int) stats.get("current_streak");
                    if (stats.containsKey("total_duration_minutes")) totalDuration = (int) stats.get("total_duration_minutes");
                    if (stats.containsKey("total_workouts")) totalWorkouts = (int) stats.get("total_workouts");
                }

                // 3. Calculate Score (0-100)

                // Factor A: Frequency (Max 60 pts)
                // Target: 12 workouts/month (3 times a week)
                double frequencyScore = Math.min((workoutsLast30Days / 12.0) * 60, 60.0);

                // Factor B: Streak (Max 20 pts)
                // Target: 10 day streak
                double streakScore = Math.min((currentStreak / 10.0) * 20, 20.0);

                // Factor C: Effort (Max 20 pts)
                // Target: Average 45 mins per workout
                double avgDuration = totalWorkouts > 0 ? (double) totalDuration / totalWorkouts : 0;
                double effortScore = Math.min((avgDuration / 45.0) * 20, 20.0);

                int totalScore = (int) (frequencyScore + streakScore + effortScore);

                // 4. Determine Styling
                String subtitle;
                int colorCode;

                if (totalScore >= 80) {
                    subtitle = "Elite Athlete status! You are crushing your goals.";
                    colorCode = Color.parseColor("#4CAF50"); // Green
                } else if (totalScore >= 60) {
                    subtitle = "Excellent consistency. Keep pushing to the next level!";
                    colorCode = Color.parseColor("#FFD300"); // Yellow (Brand Color)
                } else if (totalScore >= 40) {
                    subtitle = "Good start. Try to workout at least 3 times a week.";
                    colorCode = Color.parseColor("#FF9800"); // Orange
                } else {
                    subtitle = "Start your journey today. Consistency is the key to success!";
                    colorCode = Color.parseColor("#FF5722"); // Red
                }

                // 5. Update UI
                if (!isAdded() || getActivity() == null) return;

                int finalScore = totalScore;
                getActivity().runOnUiThread(() -> {
                    if (healthGradeCard == null) return;

                    // Locate views inside the included card layout
                    TextView gradeSubtitle = healthGradeCard.findViewById(R.id.health_subtitle);
                    MaterialCardView scoreCircle = healthGradeCard.findViewById(R.id.health_score_circle);

                    if (gradeSubtitle != null) {
                        gradeSubtitle.setText(subtitle);
                    }

                    if (scoreCircle != null) {
                        scoreCircle.setStrokeColor(colorCode);

                        // The TextView is the first child of the MaterialCardView in the XML
                        if (scoreCircle.getChildCount() > 0 && scoreCircle.getChildAt(0) instanceof TextView) {
                            TextView scoreText = (TextView) scoreCircle.getChildAt(0);
                            scoreText.setText(String.valueOf(finalScore));
                            scoreText.setTextColor(colorCode);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error calculating health grade", e);
            }
        });
    }

    private void loadLatestStats() {
        if (!isAdded()) return;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Fetching latest stats from database...");

                // Get latest progress stats
                Map<String, Double> stats = progressRepository.getLatestStats(memberId);

                if (!isAdded() || getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    View view = getView();
                    if (view == null) return;

                    GridLayout statsGrid = view.findViewById(R.id.stats_grid);
                    // NOTE: Do NOT clear views here to avoid race conditions.
                    // Views are cleared in loadWorkoutStatsForGrid.

                    // Get workout completion stats
                    loadWorkoutStatsForGrid(statsGrid, stats);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading latest stats", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error loading stats", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void loadWorkoutStatsForGrid(GridLayout statsGrid, Map<String, Double> progressStats) {
        executorService.execute(() -> {
            try {
                Map<String, Object> workoutStats = workoutCompletionRepository.getWorkoutStats(memberId);

                if (!isAdded() || getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // FIX: Clear views here immediately before adding new ones
                    statsGrid.removeAllViews();

                    // Define icons
                    int[] icons = {
                            R.drawable.workout,      // Total Days
                            R.drawable.ic_weight,    // Weight
                            R.drawable.strength,     // Bicep
                            R.drawable.ic_hip,       // Hip
                            R.drawable.ic_chest,     // Chest
                            R.drawable.heartrate     // Streak
                    };

                    // Define labels
                    String[] labels = {
                            "Workout Days",
                            "Weight (kg)",
                            "Bicep (cm)",
                            "Hip (cm)",
                            "Chest (cm)",
                            "Current Streak"
                    };

                    // Get values
                    String[] values = new String[6];
                    values[0] = String.valueOf(workoutStats.getOrDefault("total_days", 0));
                    values[1] = String.format("%.1f", progressStats.getOrDefault("weight", 0.0));
                    values[2] = String.format("%.1f", progressStats.getOrDefault("bicep", 0.0));
                    values[3] = String.format("%.1f", progressStats.getOrDefault("hip", 0.0));
                    values[4] = String.format("%.1f", progressStats.getOrDefault("chest", 0.0));
                    values[5] = String.valueOf(workoutStats.getOrDefault("current_streak", 0));

                    // Create stat items
                    for (int i = 0; i < values.length; i++) {
                        View statItem = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_stat, statsGrid, false);

                        ((ImageView) statItem.findViewById(R.id.stat_icon))
                                .setImageResource(icons[i]);
                        ((TextView) statItem.findViewById(R.id.stat_value))
                                .setText(values[i]);
                        ((TextView) statItem.findViewById(R.id.stat_label))
                                .setText(labels[i]);

                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = 0;
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                        params.setMargins(8, 8, 8, 8);
                        statItem.setLayoutParams(params);

                        statsGrid.addView(statItem);
                    }

                    Log.d(TAG, "✅ Stats grid populated successfully");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading workout stats for grid", e);
            }
        });
    }

    private void loadWorkoutStats() {
        executorService.execute(() -> {
            try {
                Map<String, Object> workoutStats = workoutCompletionRepository.getWorkoutStats(memberId);
                // Logging for debug purposes
                Log.d(TAG, "Workout Stats loaded: " + workoutStats.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error loading workout stats", e);
            }
        });
    }

    private void loadChartData(String period, int days) {
        if (!isAdded()) return;

        currentPeriod = period;
        currentDays = days;

        Log.d(TAG, "Loading chart data for period: " + period + " (" + days + " days)");

        executorService.execute(() -> {
            try {
                // Get progress data from database
                List<Map<String, Object>> weightProgress =
                        progressRepository.getWeightProgress(memberId, days);
                List<Map<String, Object>> bicepProgress =
                        progressRepository.getBicepProgress(memberId, days);
                List<Map<String, Object>> hipProgress =
                        progressRepository.getHipProgress(memberId, days);
                List<Map<String, Object>> chestProgress =
                        progressRepository.getChestProgress(memberId, days);

                // Convert to chart entries
                List<Entry> weightEntries = convertToEntries(weightProgress);
                List<Entry> bicepEntries = convertToEntries(bicepProgress);
                List<Entry> hipEntries = convertToEntries(hipProgress);
                List<Entry> chestEntries = convertToEntries(chestProgress);

                if (!isAdded() || getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    setupChart(weightChart, weightEntries, "Weight (kg)", Color.parseColor("#FFD300"));
                    setupChart(bicepChart, bicepEntries, "Bicep (cm)", Color.parseColor("#FF5722"));
                    setupChart(hipChart, hipEntries, "Hip (cm)", Color.parseColor("#009688"));
                    setupChart(chestChart, chestEntries, "Chest (cm)", Color.parseColor("#3F51B5"));
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading chart data", e);
            }
        });
    }

    private List<Entry> convertToEntries(List<Map<String, Object>> progress) {
        List<Entry> entries = new ArrayList<>();

        if (progress == null || progress.isEmpty()) {
            return entries;
        }

        // Reverse to show oldest first
        Collections.reverse(progress);

        for (int i = 0; i < progress.size(); i++) {
            Map<String, Object> item = progress.get(i);
            Object valueObj = item.get("value");

            if (valueObj != null) {
                double value = 0.0;
                if (valueObj instanceof Double) {
                    value = (Double) valueObj;
                } else if (valueObj instanceof Integer) {
                    value = ((Integer) valueObj).doubleValue();
                } else if (valueObj instanceof String) {
                    try {
                        value = Double.parseDouble((String) valueObj);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                entries.add(new Entry(i, (float) value));
            }
        }
        return entries;
    }

    private void setupChart(LineChart chart, List<Entry> entries, String label, int color) {
        if (chart == null) return;

        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No data available for " + label);
            chart.setNoDataTextColor(Color.WHITE);
            chart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.TRANSPARENT);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, Color.red(color),
                Color.green(color), Color.blue(color)));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Chart styling
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.BLACK);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.argb(50, 255, 255, 255));
        xAxis.setGranularity(1f);

        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(Color.argb(50, 255, 255, 255));

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.animateX(500);
        chart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (memberId != -1) {
            loadAllData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}