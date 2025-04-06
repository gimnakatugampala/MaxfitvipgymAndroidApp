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
        LinearLayout card = new LinearLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);
        card.setLayoutParams(params);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(Color.parseColor(isRestDay ? "#444444" : "#333333"));

        TextView dayText = new TextView(getContext());
        dayText.setText(day);
        dayText.setTextColor(isRestDay ? Color.LTGRAY : Color.YELLOW);
        dayText.setTextSize(16);
        dayText.setTypeface(null, Typeface.BOLD);
        card.addView(dayText);

        TextView summaryText = new TextView(getContext());
        summaryText.setText(summary);
        summaryText.setTextColor(Color.WHITE);
        card.addView(summaryText);

        if (total != null) {
            TextView totalText = new TextView(getContext());
            totalText.setText(total);
            totalText.setTextColor(Color.LTGRAY);
            card.addView(totalText);
        }

        workoutScheduleContainer.addView(card);
    }

    private void addWorkout(String day, String summary, String total) {
        addWorkout(day, summary, total, false);
    }
}
