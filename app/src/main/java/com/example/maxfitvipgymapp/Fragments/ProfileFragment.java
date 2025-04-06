package com.example.maxfitvipgymapp.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import androidx.fragment.app.Fragment;

import com.example.maxfitvipgymapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

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
