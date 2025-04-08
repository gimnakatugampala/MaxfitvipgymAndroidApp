package com.example.maxfitvipgymapp.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.maxfitvipgymapp.Activity.WorkoutActivity;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Service.WorkoutForegroundService;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    private LinearLayout metricsContainer;
    private LinearLayout workoutScheduleContainer;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        metricsContainer = view.findViewById(R.id.metrics_container);
        workoutScheduleContainer = view.findViewById(R.id.workout_schedule_container);
        MaterialButton startWorkoutButton = view.findViewById(R.id.startWorkoutButton);

        startWorkoutButton.setOnClickListener(v -> {
            // You can show a Toast for now or start a new Fragment/Activity
            Toast.makeText(getContext(), "Workout Started!", Toast.LENGTH_SHORT).show();

            // TODO: Replace with actual logic to start the workout sequence.
            // For example, navigate to WorkoutFragment or open a Workout dialog
            showWorkoutStartDialog(); // Or startWorkoutSequence();
        });

        // Add Health Metrics
        addMetric("Blood Pressure", "120/80", "mmHg", R.drawable.heartrate);
        addMetric("Heart Rate", "75", "bpm", R.drawable.heart);
        addMetric("Calories Burned", "450", "kcal", R.drawable.calories);
        addMetric("Steps Taken", "10,000", "steps", R.drawable.running);

        // Add Workout Schedule
        addWorkout("Mon", "2 Workouts", "Total: 85 min");
        addWorkout("Tue", "2 Workouts", "Total: 75 min");
        addWorkout("Wed", "Rest Day", null, true);
        addWorkout("Thu", "2 Workouts", "Total: 95 min");
        addWorkout("Fri", "2 Workouts", "Total: 90 min");
        addWorkout("Sat", "2 Workouts", "Total: 90 min");
        addWorkout("Sun", "Rest Day", null,true);

        return view;
    }

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
        titleText.setTextSize(18);  // Increased from 14 to 18
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setGravity(Gravity.CENTER);
        layout.addView(titleText);

        TextView valueText = new TextView(getContext());
        valueText.setText(value + " " + unit);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(22);  // Increased from 18 to 22
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

        // Make it clickable
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

        String[] workouts = new String[]{
                "Warm-up (10 min)", "Cardio (30 min)", "Strength Training (25 min)", "Cool-down (10 min)"
        };

        int[] icons = new int[]{
                R.drawable.warmup, R.drawable.heartrate, R.drawable.strength, R.drawable.cooldown
        };

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
            // Start the WorkoutActivity to show workout details
            Intent workoutActivityIntent = new Intent(getActivity(), WorkoutActivity.class);
            startActivity(workoutActivityIntent);

            Intent serviceIntent = new Intent(getActivity(), WorkoutForegroundService.class);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, "Sample Workout");
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, 600);  // Example: 10 minutes in seconds
            ContextCompat.startForegroundService(getActivity(), serviceIntent);

            // Optional: Show a Toast message to confirm workout has started
//            Toast.makeText(getContext(), "Workout Started!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


}
