package com.example.maxfitvipgymapp.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.Adapter.CalendarMonthAdapter;
import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.Model.MonthModel;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Service.WorkoutForegroundService;
import com.example.maxfitvipgymapp.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private LinearLayout metricsContainer;
    private LinearLayout workoutScheduleContainer;
    private SessionManager sessionManager;
    private TextView homeTitle;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // ✅ Initialize SessionManager
        sessionManager = new SessionManager(getContext());

        metricsContainer = view.findViewById(R.id.metrics_container);
        workoutScheduleContainer = view.findViewById(R.id.workout_schedule_container);
        homeTitle = view.findViewById(R.id.home_title);
        MaterialButton startWorkoutButton = view.findViewById(R.id.startWorkoutButton);

        // ✅ Set personalized welcome message
        setWelcomeMessage();

        // Update streak badge with current streak
        CardView streakBadge = view.findViewById(R.id.streakBadge);
        if (streakBadge != null) {
            // Get current streak from SharedPreferences
            SharedPreferences prefs = getActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
            int currentStreak = prefs.getInt("currentStreak", 0);

            // Update the streak number in the badge
            TextView streakText = view.findViewById(R.id.streakNumber);
            if (streakText != null) {
                streakText.setText(String.valueOf(currentStreak));
            }

            streakBadge.setOnClickListener(v -> showStreakDialog());
        }

        startWorkoutButton.setOnClickListener(v -> showWorkoutStartDialog());

        // Add Data
        addMetric("Blood Pressure", "120/80", "mmHg", R.drawable.heartrate);
        addMetric("Heart Rate", "75", "bpm", R.drawable.heart);
        addMetric("Calories Burned", "450", "kcal", R.drawable.calories);
        addMetric("Steps Taken", "10,000", "steps", R.drawable.running);

        addWorkout("Mon", "2 Workouts", "Total: 85 min");
        addWorkout("Tue", "2 Workouts", "Total: 75 min");
        addWorkout("Wed", "Rest Day", null, true);
        addWorkout("Thu", "2 Workouts", "Total: 95 min");
        addWorkout("Fri", "2 Workouts", "Total: 90 min");
        addWorkout("Sat", "2 Workouts", "Total: 90 min");
        addWorkout("Sun", "Rest Day", null, true);

        return view;
    }

    // ✅ NEW METHOD: Set personalized welcome message
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
        // Update streak when fragment resumes
        updateStreakDisplay();
        // ✅ Update welcome message when fragment resumes
        setWelcomeMessage();
    }

    private void updateStreakDisplay() {
        View view = getView();
        if (view != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
            int currentStreak = prefs.getInt("currentStreak", 0);

            // Update streak badge
            TextView streakNumber = view.findViewById(R.id.streakNumber);
            if (streakNumber != null) {
                streakNumber.setText(String.valueOf(currentStreak));
            }

            // Update streak card
            updateStreakCard(currentStreak);
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

        // Update dialog title with current streak
        SharedPreferences prefs = getActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);

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
    }

    private void setupStreakCalendar(RecyclerView recyclerView) {
        List<MonthModel> monthList = new ArrayList<>();

        // Get workout history from SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE);
        String lastWorkoutDate = prefs.getString("lastWorkoutDate", "");
        int currentStreak = prefs.getInt("currentStreak", 0);

        // Create a set of completed workout dates
        Set<String> completedDates = new HashSet<>();
        if (!lastWorkoutDate.isEmpty() && currentStreak > 0) {
            Calendar cal = Calendar.getInstance();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                cal.setTime(sdf.parse(lastWorkoutDate));

                // Add all dates in the current streak
                for (int i = 0; i < currentStreak; i++) {
                    completedDates.add(sdf.format(cal.getTime()));
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Start Date: 5 months ago
        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.MONTH, -5);
        startCal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar endCal = Calendar.getInstance();
        Calendar iteratorCal = (Calendar) startCal.clone();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Loop through months
        while (iteratorCal.before(endCal) ||
                (iteratorCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
                        iteratorCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR))) {

            List<DateModel> daysInMonth = new ArrayList<>();
            String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(iteratorCal.getTime());
            int maxDays = iteratorCal.getActualMaximum(Calendar.DAY_OF_MONTH);

            iteratorCal.set(Calendar.DAY_OF_MONTH, 1);
            int startDayOfWeek = iteratorCal.get(Calendar.DAY_OF_WEEK);

            // Spacers
            for (int i = 1; i < startDayOfWeek; i++) {
                daysInMonth.add(new DateModel("", "", false, true));
            }

            // Days
            for (int day = 1; day <= maxDays; day++) {
                Calendar currentDay = (Calendar) iteratorCal.clone();
                currentDay.set(Calendar.DAY_OF_MONTH, day);
                boolean isFuture = currentDay.after(endCal);

                // Check if this date is in completed dates
                String dateString = dateFormatter.format(currentDay.getTime());
                boolean attended = completedDates.contains(dateString);

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
        view.animate().rotation(0f).setDuration(600).start();
    }

    // --- METRICS & WORKOUT METHODS ---

    private void addMetric(String title, String value, String unit, int iconRes) {
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
        card.addView(layout);
        metricsContainer.addView(card);
    }

    private void addWorkout(String day, String summary, String total, boolean isRestDay) {
        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        workoutScheduleContainer.addView(card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showWorkoutDialog(day, isRestDay));
    }

    private void showWorkoutDialog(String day, boolean isRestDay) {
        if (isRestDay) {
            Toast.makeText(getContext(), day + " is a rest day. Take it easy and recharge!", Toast.LENGTH_LONG).show();
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_workout_details, null);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText(day + " - Workouts");
        LinearLayout contentContainer = dialogView.findViewById(R.id.dialog_content_container);
        String[] workouts = new String[]{"Warm-up (10 min)", "Cardio (30 min)", "Strength Training (25 min)", "Cool-down (10 min)"};
        int[] icons = new int[]{R.drawable.warmup, R.drawable.heartrate, R.drawable.strength, R.drawable.cooldown};
        for (int i = 0; i < workouts.length; i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);
            ImageView icon = new ImageView(getContext());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
            iconParams.setMarginEnd(20);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(Color.YELLOW);
            TextView workoutText = new TextView(getContext());
            workoutText.setText(workouts[i]);
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

    private void addWorkout(String day, String summary, String total) {
        addWorkout(day, summary, total, false);
    }

    private void showWorkoutStartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Start Workout");
        builder.setMessage("Are you ready to begin your workout?");
        builder.setPositiveButton("Start", (dialog, which) -> {
            Intent workoutActivityIntent = new Intent(getActivity(), WorkoutActivity.class);
            startActivity(workoutActivityIntent);
            Intent serviceIntent = new Intent(getActivity(), WorkoutForegroundService.class);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, "Sample Workout");
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, 600);
            ContextCompat.startForegroundService(getActivity(), serviceIntent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}