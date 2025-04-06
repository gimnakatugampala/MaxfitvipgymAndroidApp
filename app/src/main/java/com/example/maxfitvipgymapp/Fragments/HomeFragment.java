package com.example.maxfitvipgymapp.Fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.maxfitvipgymapp.R;

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

        // Optional: Add icon
        ImageView icon = new ImageView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(isRestDay ? R.drawable.resting : R.drawable.workout); // Use your custom icons
        icon.setColorFilter(Color.YELLOW);
        layout.addView(icon);

        // Text container
        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(20, 0, 0, 0);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView dayText = new TextView(getContext());
        dayText.setText(day);
        dayText.setTextColor(Color.WHITE);
        dayText.setTextSize(20);  // Increased from 18 to 20
        dayText.setTypeface(Typeface.DEFAULT_BOLD);
        textLayout.addView(dayText);

        TextView summaryText = new TextView(getContext());
        summaryText.setText(summary);
        summaryText.setTextColor(Color.LTGRAY);
        summaryText.setTextSize(17);  // Increased from 15 to 17
        textLayout.addView(summaryText);

        if (total != null) {
            TextView totalText = new TextView(getContext());
            totalText.setText(total);
            totalText.setTextColor(Color.GRAY);
            totalText.setTextSize(16);  // Increased from 14 to 16
            textLayout.addView(totalText);
        }

        layout.addView(textLayout);
        card.addView(layout);
        workoutScheduleContainer.addView(card);
    }


    private void addWorkout(String day, String summary, String total) {
        addWorkout(day, summary, total, false);
    }
}
