package com.example.maxfitvipgymapp.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.view.ViewGroup.LayoutParams;
import androidx.fragment.app.Fragment;
import com.example.maxfitvipgymapp.R;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

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

        return view;
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
