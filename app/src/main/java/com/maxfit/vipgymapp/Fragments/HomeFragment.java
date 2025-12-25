package com.maxfit.vipgymapp.Fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxfit.vipgymapp.Activity.WorkoutActivity;
import com.maxfit.vipgymapp.Activity.WorkoutSettingsActivity;
import com.maxfit.vipgymapp.Adapter.CalendarMonthAdapter;
import com.maxfit.vipgymapp.Model.DateModel;
import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.Model.MonthModel;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.WorkoutRepository;
import com.maxfit.vipgymapp.Service.HealthTrackerService;
import com.maxfit.vipgymapp.Service.WorkoutForegroundService;
import com.maxfit.vipgymapp.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.maxfit.vipgymapp.Repository.WorkoutCompletionRepository;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final long SCHEDULE_REFRESH_INTERVAL = 30000; // 30 seconds

    private LinearLayout metricsContainer;
    private LinearLayout workoutScheduleContainer;
    private SessionManager sessionManager;
    private TextView homeTitle;
    private WorkoutRepository workoutRepository;
    private ExecutorService executorService;
    private MaterialButton startWorkoutButton;

    // ✅ Health data variables
    private int currentSteps = 0;
    private double currentDistance = 0.0;
    private int currentCalories = 0;
    private int currentActiveMinutes = 0;

    // ✅ Health metric TextViews for updating
    private TextView stepsValueText;
    private TextView distanceValueText;
    private TextView caloriesValueText;
    private TextView activeMinutesValueText;

    // ✅ Schedule refresh handler
    private Handler scheduleRefreshHandler;
    private Runnable scheduleRefreshRunnable;

    // ✅ FIXED: Health data broadcast receiver (Standardized Name)
    private final BroadcastReceiver healthDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (HealthTrackerService.ACTION_HEALTH_UPDATE.equals(intent.getAction())) {
                // Get data from the broadcast
                int steps = intent.getIntExtra("steps", 0);
                double distance = intent.getDoubleExtra("distance_km", 0.0);
                int calories = intent.getIntExtra("calories", 0);
                int activeMinutes = intent.getIntExtra("active_minutes", 0);

                // Update local variables
                currentSteps = steps;
                currentDistance = distance;
                currentCalories = calories;
                currentActiveMinutes = activeMinutes;

                // Update the UI immediately
                updateHealthMetrics();
            }
        }
    };

    // Store schedule data
    private int currentMemberScheduleId = -1;
    private Map<String, List<Map<String, Object>>> weekWorkoutsMap = new HashMap<>();
    private boolean hasActiveSchedule = false;
    private boolean isTodayRestDay = false;
    private List<Map<String, Object>> todayWorkouts = null;

    private WorkoutCompletionRepository workoutCompletionRepository;

    // Day name mapping
    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] DAY_ABBR = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize
        sessionManager = new SessionManager(getContext());
        workoutRepository = new WorkoutRepository();
        executorService = Executors.newSingleThreadExecutor();

        metricsContainer = view.findViewById(R.id.metrics_container);
        workoutScheduleContainer = view.findViewById(R.id.workout_schedule_container);
        homeTitle = view.findViewById(R.id.home_title);
        startWorkoutButton = view.findViewById(R.id.startWorkoutButton);

        workoutCompletionRepository = new WorkoutCompletionRepository();

        // Set personalized welcome message
        setWelcomeMessage();

        // Update streak badge
        CardView streakBadge = view.findViewById(R.id.streakBadge);
        if (streakBadge != null && getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
            int currentStreak = prefs.getInt("currentStreak", 0);

            TextView streakText = view.findViewById(R.id.streakNumber);
            if (streakText != null) {
                streakText.setText(String.valueOf(currentStreak));
            }

            streakBadge.setOnClickListener(v -> showStreakDialog());
        }

        // Workout button
        startWorkoutButton.setOnClickListener(v -> showWorkoutStartDialog());

        // Add mic icon in header
        addMicIconToHeader(view);

        // ✅ Add AUTO-TRACKED health metrics (no manual input!)
        addMetricWithUpdate("Steps", "0", "steps", R.drawable.running);
        addMetricWithUpdate("Distance", "0.0", "km", R.drawable.running);
        addMetricWithUpdate("Calories", "0", "kcal", R.drawable.calories);
        addMetricWithUpdate("Active Time", "0", "min", R.drawable.heartrate);

        // ✅ Start health tracker service
        startHealthTrackerService();

        // ✅ Load initial health data from storage
        loadInitialHealthData();

        // Load workout schedule from database
        loadWorkoutScheduleFromDB();

        // ✅ START: Setup automatic schedule refresh
        setupScheduleRefresh();

        return view;
    }

    // ✅ NEW: Setup periodic schedule refresh
    private void setupScheduleRefresh() {
        scheduleRefreshHandler = new Handler(Looper.getMainLooper());
        scheduleRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && getActivity() != null) {
                    Log.d(TAG, "🔄 Auto-refreshing workout schedule...");
                    loadWorkoutScheduleFromDB();

                    // Schedule next refresh
                    scheduleRefreshHandler.postDelayed(this, SCHEDULE_REFRESH_INTERVAL);
                }
            }
        };

        // Start periodic refresh
        scheduleRefreshHandler.postDelayed(scheduleRefreshRunnable, SCHEDULE_REFRESH_INTERVAL);
        Log.d(TAG, "✅ Schedule auto-refresh enabled (every 30 seconds)");
    }

    // ✅ NEW: Manual refresh method
    public void refreshSchedule() {
        if (isAdded() && getActivity() != null) {
            Log.d(TAG, "🔄 Manual schedule refresh triggered");
            loadWorkoutScheduleFromDB();
            Toast.makeText(getContext(), "Schedule refreshed", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ NEW: Load initial health data
    private void loadInitialHealthData() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("HealthTrackerPrefs", Context.MODE_PRIVATE);
            String today = getTodayDate();
            String lastDate = prefs.getString("last_date", "");

            if (today.equals(lastDate)) {
                currentSteps = prefs.getInt("today_steps", 0);
                Log.d(TAG, "📱 Loaded initial steps from storage: " + currentSteps);

                // Calculate derived metrics manually for initial display
                updateDerivedMetrics();
                updateHealthMetrics();
            } else {
                Log.d(TAG, "📱 No data for today yet, waiting for sensor updates...");
            }
        }
    }

    // ✅ NEW: Calculate derived metrics (same logic as in service)
    private void updateDerivedMetrics() {
        if (getContext() == null) return;

        // Calculate stride length
        int userHeightCm = 170; // Default
        String userGender = "M";

        SharedPreferences userPrefs = getContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        userHeightCm = userPrefs.getInt("height_cm", 170);
        userGender = userPrefs.getString("gender", "M");

        double heightMeters = userHeightCm / 100.0;
        double strideLength;
        if (userGender.equals("M")) {
            strideLength = heightMeters * 0.415;
        } else {
            strideLength = heightMeters * 0.413;
        }

        // Calculate distance
        currentDistance = (currentSteps * strideLength) / 1000.0;

        // Calculate calories (simplified for UI, service has full BMR logic)
        currentCalories = 1500 + (int) (currentSteps * 0.04);

        // Calculate active minutes
        currentActiveMinutes = currentSteps / 100;

        Log.d(TAG, "📊 Calculated metrics - Steps: " + currentSteps +
                ", Distance: " + String.format("%.2f", currentDistance) + " km" +
                ", Calories: " + currentCalories +
                ", Active: " + currentActiveMinutes + " min");
    }

    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private String getTodayDayName() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return DAYS[dayOfWeek - 1];
    }

    private String getTodayDayAbbr() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return DAY_ABBR[dayOfWeek - 1];
    }

    private void loadWorkoutScheduleFromDB() {
        int memberId = sessionManager.getMemberId();

        if (memberId == -1) {
            Log.e(TAG, "Member ID not found in session");
            showEmptySchedule();
            disableWorkoutButton("No member session");
            return;
        }

        // Show loading state
        if (workoutScheduleContainer != null) {
            workoutScheduleContainer.removeAllViews();
            ProgressBar progressBar = new ProgressBar(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            params.setMargins(0, 40, 0, 40);
            progressBar.setLayoutParams(params);
            workoutScheduleContainer.addView(progressBar);
        }

        executorService.execute(() -> {
            try {
                // Get member's current active workout schedule
                Map<String, Object> memberSchedule = workoutRepository.getMemberWorkoutSchedule(memberId);

                // ✅ Check if fragment is still attached before updating UI
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment not attached, skipping UI update");
                    return;
                }

                if (memberSchedule == null) {
                    Log.d(TAG, "No active workout schedule found for member");
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            showEmptySchedule();
                            disableWorkoutButton("No schedule assigned");
                        }
                    });
                    return;
                }

                currentMemberScheduleId = (int) memberSchedule.get("id");
                hasActiveSchedule = true;
                Log.d(TAG, "✅ Found active schedule ID: " + currentMemberScheduleId);

                // Get today's day name
                String todayDayName = getTodayDayName();
                String todayDayAbbr = getTodayDayAbbr();
                Log.d(TAG, "Today is: " + todayDayName);

                // Build schedule for each day of the week
                List<DaySchedule> weekSchedule = new ArrayList<>();
                weekWorkoutsMap.clear();

                for (int i = 0; i < 7; i++) {
                    String dayName = DAYS[i];
                    String dayAbbr = DAY_ABBR[i];

                    // Get workouts for this day
                    List<Map<String, Object>> dayWorkouts =
                            workoutRepository.getMemberWorkoutScheduleDetails(currentMemberScheduleId, dayName);

                    // Store workouts for this day
                    weekWorkoutsMap.put(dayAbbr, dayWorkouts);

                    // Check if this is today and store today's workouts
                    if (dayName.equals(todayDayName)) {
                        todayWorkouts = dayWorkouts;
                    }

                    // Check if it's a rest day
                    boolean isRestDay = false;
                    int totalDuration = 0;
                    int workoutCount = 0;

                    if (dayWorkouts != null && !dayWorkouts.isEmpty()) {
                        for (Map<String, Object> workout : dayWorkouts) {
                            Boolean restDay = (Boolean) workout.get("is_rest_day");
                            if (restDay != null && restDay) {
                                isRestDay = true;
                                break;
                            }

                            // Calculate total duration
                            String durationStr = (String) workout.get("duration_minutes");
                            if (durationStr != null && !durationStr.isEmpty()) {
                                try {
                                    totalDuration += Integer.parseInt(durationStr);
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Invalid duration: " + durationStr);
                                }
                            }
                            workoutCount++;
                        }
                    } else {
                        // No workouts assigned = rest day
                        isRestDay = true;
                    }

                    // Set today's rest day status
                    if (dayName.equals(todayDayName)) {
                        isTodayRestDay = isRestDay;
                    }

                    weekSchedule.add(new DaySchedule(dayAbbr, workoutCount, totalDuration, isRestDay));
                }

                // ✅ Update UI on main thread with null check
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && workoutScheduleContainer != null) {
                            workoutScheduleContainer.removeAllViews();
                            for (DaySchedule day : weekSchedule) {
                                if (day.isRestDay) {
                                    addWorkout(day.dayName, "Rest Day", null, true);
                                } else if (day.workoutCount > 0) {
                                    String summary = day.workoutCount + (day.workoutCount == 1 ? " Workout" : " Workouts");
                                    String total = "Total: " + day.totalDuration + " min";
                                    addWorkout(day.dayName, summary, total, false);
                                } else {
                                    addWorkout(day.dayName, "No Workouts", null, true);
                                }
                            }

                            // Update button state based on today's schedule
                            updateWorkoutButtonState();

                            // ✅ Show refresh confirmation
                            Log.d(TAG, "✅ Schedule UI updated successfully");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading workout schedule", e);
                // ✅ FIXED: Check if fragment is attached before updating UI
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            showEmptySchedule();
                            disableWorkoutButton("Error loading schedule");
                            Toast.makeText(getContext(), "Could not load workout schedule. Check your internet connection.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void updateWorkoutButtonState() {
        if (!hasActiveSchedule) {
            disableWorkoutButton("No schedule assigned");
            return;
        }

        if (isTodayRestDay) {
            disableWorkoutButton("Today is a rest day");
            return;
        }

        if (todayWorkouts == null || todayWorkouts.isEmpty()) {
            disableWorkoutButton("No workouts for today");
            return;
        }

        // Has schedule and workouts for today
        enableWorkoutButton();
    }

    private void enableWorkoutButton() {
        if (startWorkoutButton != null && isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && startWorkoutButton != null) {
                    startWorkoutButton.setEnabled(true);
                    startWorkoutButton.setAlpha(1.0f);
                    Log.d(TAG, "Workout button ENABLED");
                }
            });
        }
    }

    private void disableWorkoutButton(String reason) {
        if (startWorkoutButton != null && isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && startWorkoutButton != null) {
                    startWorkoutButton.setEnabled(false);
                    startWorkoutButton.setAlpha(0.5f);
                    Log.d(TAG, "Workout button DISABLED: " + reason);
                }
            });
        }
    }

    private static class DaySchedule {
        String dayName;
        int workoutCount;
        int totalDuration;
        boolean isRestDay;

        DaySchedule(String dayName, int workoutCount, int totalDuration, boolean isRestDay) {
            this.dayName = dayName;
            this.workoutCount = workoutCount;
            this.totalDuration = totalDuration;
            this.isRestDay = isRestDay;
        }
    }

    private void showEmptySchedule() {
        if (workoutScheduleContainer != null && isAdded()) {
            workoutScheduleContainer.removeAllViews();

            TextView emptyText = new TextView(getContext());
            emptyText.setText("No workout schedule assigned yet.\nContact your trainer to get started!");
            emptyText.setTextColor(Color.parseColor("#AAAAAA"));
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(32, 64, 32, 64);

            workoutScheduleContainer.addView(emptyText);
        }
    }

    private void addMicIconToHeader(View view) {
        LinearLayout homeContainer = view.findViewById(R.id.home_container);
        if (homeContainer == null || homeContainer.getChildCount() == 0) return;

        View firstChild = homeContainer.getChildAt(0);
        if (!(firstChild instanceof LinearLayout)) return;

        LinearLayout headerLayout = (LinearLayout) firstChild;

        ImageView micIcon = new ImageView(getContext());
        int iconSize = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconSize, iconSize);
        params.setMargins(
                (int) (8 * getResources().getDisplayMetrics().density),
                0,
                (int) (8 * getResources().getDisplayMetrics().density),
                0
        );
        micIcon.setLayoutParams(params);

        micIcon.setImageResource(R.drawable.mic);
        micIcon.setPadding(4, 4, 4, 4);

        micIcon.setClickable(true);
        micIcon.setFocusable(true);
        micIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WorkoutSettingsActivity.class);
            startActivity(intent);
        });

        micIcon.setForeground(ContextCompat.getDrawable(getContext(),
                android.R.drawable.list_selector_background));

        int insertPosition = headerLayout.getChildCount() - 1;
        if (insertPosition >= 0) {
            headerLayout.addView(micIcon, insertPosition);
        } else {
            headerLayout.addView(micIcon);
        }
    }

    private void setWelcomeMessage() {
        if (homeTitle != null) {
            Member member = sessionManager.getMemberData();
            if (member != null && member.getFirstName() != null && !member.getFirstName().isEmpty()) {
                homeTitle.setText("Welcome Back, " + member.getFirstName() + "! 👋");
            } else {
                homeTitle.setText("Welcome Back! 👋");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStreakDisplay();
        setWelcomeMessage();
        loadWorkoutScheduleFromDB();

        // ✅ Register health update receiver (CORRECTED & MOVED HERE)
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                    healthDataReceiver,
                    new IntentFilter(HealthTrackerService.ACTION_HEALTH_UPDATE)
            );
        }

        // ✅ Refresh health data from storage immediately
        loadInitialHealthData();

        // ✅ Resume schedule refresh
        if (scheduleRefreshHandler != null && scheduleRefreshRunnable != null) {
            scheduleRefreshHandler.postDelayed(scheduleRefreshRunnable, SCHEDULE_REFRESH_INTERVAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // ✅ Pause schedule refresh when fragment is not visible
        if (scheduleRefreshHandler != null && scheduleRefreshRunnable != null) {
            scheduleRefreshHandler.removeCallbacks(scheduleRefreshRunnable);
        }

        // ✅ Unregister to save battery and avoid leaks (CORRECTED)
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(healthDataReceiver);
        }
    }

    private void updateStreakDisplay() {
        View view = getView();
        if (view != null && getActivity() != null && isAdded()) {
            int memberId = sessionManager.getMemberId();

            executorService.execute(() -> {
                int currentStreak = workoutCompletionRepository.calculateCurrentStreak(memberId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        TextView streakNumber = view.findViewById(R.id.streakNumber);
                        if (streakNumber != null) {
                            streakNumber.setText(String.valueOf(currentStreak));
                        }

                        updateStreakCard(currentStreak);
                    });
                }
            });
        }
    }

    private void updateStreakCard(int streak) {
        View view = getView();
        if (view != null) {
            TextView streakCardTitle = view.findViewById(R.id.streakCardTitle);
            if (streakCardTitle != null) {
                streakCardTitle.setText(streak + "-Day Running Streak");
            }
        }
    }

    private void showStreakDialog() {
        if (!isAdded() || getActivity() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_streak, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView streakIcon = dialogView.findViewById(R.id.streak_animation_view);
        RecyclerView recyclerView = dialogView.findViewById(R.id.streakRecyclerView);
        MaterialButton btnContinue = dialogView.findViewById(R.id.btnContinue);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        int memberId = sessionManager.getMemberId();

        executorService.execute(() -> {
            int currentStreak = workoutCompletionRepository.calculateCurrentStreak(memberId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    TextView streakTitle = dialogView.findViewById(R.id.streakTitle);
                    if (streakTitle != null) {
                        streakTitle.setText(currentStreak + " Day Streak!");
                    }

                    setupStreakCalendar(recyclerView);
                    animateStreakIcon(streakIcon);

                    btnContinue.setOnClickListener(v -> dialog.dismiss());
                    if (btnClose != null) {
                        btnClose.setOnClickListener(v -> dialog.dismiss());
                    }

                    dialog.show();
                });
            }
        });
    }

    private void setupStreakCalendar(RecyclerView recyclerView) {
        if (!isAdded() || getActivity() == null) return;

        List<MonthModel> monthList = new ArrayList<>();
        int memberId = sessionManager.getMemberId();

        executorService.execute(() -> {
            List<String> completedDates = workoutCompletionRepository.getCompletionDates(memberId, 6);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    Set<String> completedSet = new HashSet<>(completedDates);

                    Calendar startCal = Calendar.getInstance();
                    startCal.add(Calendar.MONTH, -5);
                    startCal.set(Calendar.DAY_OF_MONTH, 1);

                    Calendar endCal = Calendar.getInstance();
                    Calendar iteratorCal = (Calendar) startCal.clone();
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

                    while (iteratorCal.before(endCal) ||
                            (iteratorCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
                                    iteratorCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR))) {

                        List<DateModel> daysInMonth = new ArrayList<>();
                        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(iteratorCal.getTime());
                        int maxDays = iteratorCal.getActualMaximum(Calendar.DAY_OF_MONTH);

                        iteratorCal.set(Calendar.DAY_OF_MONTH, 1);
                        int startDayOfWeek = iteratorCal.get(Calendar.DAY_OF_WEEK);

                        for (int i = 1; i < startDayOfWeek; i++) {
                            daysInMonth.add(new DateModel("", "", false, true));
                        }

                        for (int day = 1; day <= maxDays; day++) {
                            Calendar currentDay = (Calendar) iteratorCal.clone();
                            currentDay.set(Calendar.DAY_OF_MONTH, day);
                            boolean isFuture = currentDay.after(endCal);

                            String dateString = dateFormatter.format(currentDay.getTime());
                            boolean attended = completedSet.contains(dateString);

                            daysInMonth.add(new DateModel("", String.valueOf(day), attended, isFuture));
                        }

                        monthList.add(new MonthModel(monthName, daysInMonth));
                        iteratorCal.add(Calendar.MONTH, 1);
                    }

                    CalendarMonthAdapter adapter = new CalendarMonthAdapter(getContext(), monthList);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    recyclerView.setAdapter(adapter);

                    if (monthList.size() > 0) {
                        recyclerView.scrollToPosition(monthList.size() - 1);
                    }
                });
            }
        });
    }

    private void animateStreakIcon(View view) {
        if (view == null) return;
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();
        view.setRotation(-15f);
        view.animate().rotation(0f)
                .setDuration(600)
                .start();
    }

    private void addMetricWithUpdate(String title, String value, String unit, int iconRes) {
        if (!isAdded()) return;

        CardView card = new CardView(getContext());
        int cardSizeInDp = 160;
        int cardSizeInPx = (int) (cardSizeInDp * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardSizeInPx, cardSizeInPx);
        cardParams.setMargins(20, 0, 20, 0);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.parseColor("#212121"));
        card.setRadius(20);
        card.setCardElevation(10);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.YELLOW);
        layout.addView(icon);

        TextView titleText = new TextView(getContext());
        titleText.setText(title);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(18);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setGravity(Gravity.CENTER);
        layout.addView(titleText);

        TextView valueText = new TextView(getContext());
        valueText.setText(value + " " + unit);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(22);
        valueText.setTypeface(null, Typeface.BOLD);
        valueText.setGravity(Gravity.CENTER);
        layout.addView(valueText);

        // ✅ Store references for later updates
        if (title.equals("Steps")) {
            stepsValueText = valueText;
        } else if (title.equals("Distance")) {
            distanceValueText = valueText;
        } else if (title.equals("Calories")) {
            caloriesValueText = valueText;
        } else if (title.equals("Active Time")) {
            activeMinutesValueText = valueText;
        }

        card.addView(layout);

        if (metricsContainer != null) {
            metricsContainer.addView(card);
        }
    }

    // ✅ Update health metrics in UI
    private void updateHealthMetrics() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (stepsValueText != null) {
                stepsValueText.setText(String.format(Locale.US, "%,d steps", currentSteps));
            }
            if (distanceValueText != null) {
                distanceValueText.setText(String.format(Locale.US, "%.2f km", currentDistance));
            }
            if (caloriesValueText != null) {
                caloriesValueText.setText(String.format(Locale.US, "%d kcal", currentCalories));
            }
            if (activeMinutesValueText != null) {
                activeMinutesValueText.setText(String.format(Locale.US, "%d min", currentActiveMinutes));
            }

            Log.d(TAG, "✅ Health metrics UI updated");
        });
    }

    // ✅ Start health tracker service
    private void startHealthTrackerService() {
        if (getContext() != null) {
            Intent serviceIntent = new Intent(getContext(), HealthTrackerService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }
            Log.d(TAG, "✅ HealthTracker Service started");
        }
    }

    private void addWorkout(String day, String summary, String total, boolean isRestDay) {
        if (!isAdded()) return;

        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        card.setLayoutParams(params);
        card.setCardElevation(8);
        card.setRadius(16);
        card.setCardBackgroundColor(Color.parseColor(isRestDay ? "#2C2C2C" : "#333333"));

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(isRestDay ? R.drawable.resting : R.drawable.workout);
        icon.setColorFilter(Color.YELLOW);
        layout.addView(icon);

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(20, 0, 0, 0);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView dayText = new TextView(getContext());
        dayText.setText(day);
        dayText.setTextColor(Color.WHITE);
        dayText.setTextSize(20);
        dayText.setTypeface(Typeface.DEFAULT_BOLD);
        textLayout.addView(dayText);

        TextView summaryText = new TextView(getContext());
        summaryText.setText(summary);
        summaryText.setTextColor(Color.LTGRAY);
        summaryText.setTextSize(17);
        textLayout.addView(summaryText);

        if (total != null) {
            TextView totalText = new TextView(getContext());
            totalText.setText(total);
            totalText.setTextColor(Color.GRAY);
            totalText.setTextSize(16);
            textLayout.addView(totalText);
        }

        layout.addView(textLayout);
        card.addView(layout);

        if (workoutScheduleContainer != null) {
            workoutScheduleContainer.addView(card);
        }

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showWorkoutDialog(day, isRestDay));
    }

    private void showWorkoutDialog(String day, boolean isRestDay) {
        if (!isAdded()) return;

        if (isRestDay) {
            Toast.makeText(getContext(),
                    day + " is a rest day. Take it easy and recharge!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<Map<String, Object>> dayWorkouts = weekWorkoutsMap.get(day);

        if (dayWorkouts == null || dayWorkouts.isEmpty()) {
            Toast.makeText(getContext(),
                    "No workout details available for " + day,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_workout_details, null);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText(day + " - Workouts");
        LinearLayout contentContainer = dialogView.findViewById(R.id.dialog_content_container);

        for (Map<String, Object> workout : dayWorkouts) {
            String workoutName = (String) workout.get("name");
            String sets = (String) workout.get("set_no");
            String reps = (String) workout.get("rep_no");
            String duration = (String) workout.get("duration_minutes");

            if (workoutName == null) workoutName = "Workout";

            StringBuilder details = new StringBuilder(workoutName);
            if (sets != null && !sets.isEmpty()) {
                details.append(" - ").append(sets).append(" sets");
            }
            if (reps != null && !reps.isEmpty()) {
                details.append(" × ").append(reps).append(" reps");
            }
            if (duration != null && !duration.isEmpty()) {
                details.append(" (").append(duration).append(" min)");
            }

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(getContext());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
            iconParams.setMarginEnd(20);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.workout);
            icon.setColorFilter(Color.YELLOW);

            TextView workoutText = new TextView(getContext());
            workoutText.setText(details.toString());
            workoutText.setTextColor(Color.WHITE);
            workoutText.setTextSize(17);

            row.addView(icon);
            row.addView(workoutText);
            contentContainer.addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showWorkoutStartDialog() {
        if (!isAdded() || getActivity() == null) return;

        // Double-check all conditions
        if (!hasActiveSchedule) {
            Toast.makeText(getContext(),
                    "No workout schedule assigned.\nPlease contact your trainer.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (isTodayRestDay) {
            Toast.makeText(getContext(),
                    "Today is a rest day! 😴\nTake it easy and recharge for tomorrow.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (todayWorkouts == null || todayWorkouts.isEmpty()) {
            Toast.makeText(getContext(),
                    "No workouts scheduled for today.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Build workout summary
        String todayDay = getTodayDayName();
        int workoutCount = todayWorkouts.size();
        int totalDuration = 0;

        for (Map<String, Object> workout : todayWorkouts) {
            String durationStr = (String) workout.get("duration_minutes");
            if (durationStr != null && !durationStr.isEmpty()) {
                try {
                    totalDuration += Integer.parseInt(durationStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid duration: " + durationStr);
                }
            }
        }

        final int finalTotalDuration = totalDuration;

        String message = String.format(
                "Today's workout (%s):\n\n%d exercises\nTotal duration: ~%d minutes\n\nAre you ready to begin?",
                todayDay, workoutCount, finalTotalDuration
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Start Workout");
        builder.setMessage(message);
        builder.setPositiveButton("Start", (dialog, which) -> {
            Intent workoutActivityIntent = new Intent(getActivity(), WorkoutActivity.class);

            String firstWorkoutTitle = "Today's Workout";
            if (!todayWorkouts.isEmpty()) {
                String name = (String) todayWorkouts.get(0).get("name");
                if (name != null && !name.isEmpty()) {
                    firstWorkoutTitle = name;
                }
            }

            startActivity(workoutActivityIntent);

            Intent serviceIntent = new Intent(getActivity(), WorkoutForegroundService.class);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, firstWorkoutTitle);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, finalTotalDuration * 60);
            ContextCompat.startForegroundService(getActivity(), serviceIntent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // ✅ Stop schedule refresh
        if (scheduleRefreshHandler != null && scheduleRefreshRunnable != null) {
            scheduleRefreshHandler.removeCallbacks(scheduleRefreshRunnable);
            Log.d(TAG, "✅ Schedule refresh stopped");
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}