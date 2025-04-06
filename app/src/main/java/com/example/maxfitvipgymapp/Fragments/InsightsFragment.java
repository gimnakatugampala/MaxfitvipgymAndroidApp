package com.example.maxfitvipgymapp.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.gridlayout.widget.GridLayout;

import com.example.maxfitvipgymapp.Adapter.DateAdapter;
import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class InsightsFragment extends Fragment {

    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;

    private LineChart weightChart, bicepChart, hipChart, chestChart;
    private Button btnWeekly, btnMonthly, btnYearly;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        // Chart and button references
        weightChart = view.findViewById(R.id.weight_chart);
        bicepChart = view.findViewById(R.id.bicep_chart);
        hipChart = view.findViewById(R.id.hip_chart);
        chestChart = view.findViewById(R.id.chest_chart);

        btnWeekly = view.findViewById(R.id.btn_weekly);
        btnMonthly = view.findViewById(R.id.btn_monthly);
        btnYearly = view.findViewById(R.id.btn_yearly);

        // Load initial chart data
        loadChartData("Weekly");

        btnWeekly.setOnClickListener(v -> loadChartData("Weekly"));
        btnMonthly.setOnClickListener(v -> loadChartData("Monthly"));
        btnYearly.setOnClickListener(v -> loadChartData("Yearly"));

        // Date selector setup
        dateRecyclerView = view.findViewById(R.id.date_recycler_view);
        dateRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        List<DateModel> dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance(); // Start from today
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());
            boolean isSelected = (i == 0);
            dateList.add(new DateModel(day, date, isSelected, false));
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        Collections.reverse(dateList);
        int selectedIndex = dateList.size() - 1;
        dateRecyclerView.scrollToPosition(selectedIndex);

        dateAdapter = new DateAdapter(dateList, position -> dateAdapter.setSelected(position));
        dateRecyclerView.setAdapter(dateAdapter);
        dateAdapter.setSelected(selectedIndex);

        // Stats grid
        GridLayout statsGrid = view.findViewById(R.id.stats_grid);
        int[] icons = {
                R.drawable.ic_calories,
                R.drawable.ic_weight,
                R.drawable.strength,
                R.drawable.ic_hip,
                R.drawable.ic_chest,
                R.drawable.heartrate
        };
        String[] values = {"620.98", "70.98", "35.5", "98.2", "105.3", "180"};
        String[] labels = {"Calories", "Weight (kg)", "Bicep Size (cm)", "Hip Size (cm)", "Chest Size (cm)", "MAX H/R"};

        for (int i = 0; i < values.length; i++) {
            View statItem = LayoutInflater.from(getContext()).inflate(R.layout.item_stat, statsGrid, false);

            ((ImageView) statItem.findViewById(R.id.stat_icon)).setImageResource(icons[i]);
            ((TextView) statItem.findViewById(R.id.stat_value)).setText(values[i]);
            ((TextView) statItem.findViewById(R.id.stat_label)).setText(labels[i]);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            statItem.setLayoutParams(params);

            statsGrid.addView(statItem);
        }

        return view;
    }

    private void loadChartData(String period) {
        // Example mock data
        List<Entry> weightEntries = new ArrayList<>();
        List<Entry> bicepEntries = new ArrayList<>();
        List<Entry> hipEntries = new ArrayList<>();
        List<Entry> chestEntries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            weightEntries.add(new Entry(i, 60 + i));
            bicepEntries.add(new Entry(i, 30 + i * 0.5f));
            hipEntries.add(new Entry(i, 90 + i));
            chestEntries.add(new Entry(i, 100 + i * 1.2f));
        }

        setupChart(weightChart, weightEntries, "Weight", Color.parseColor("#FFD300")); // Yellow
        setupChart(bicepChart, bicepEntries, "Bicep", Color.parseColor("#FF5722"));    // Orange
        setupChart(hipChart, hipEntries, "Hip", Color.parseColor("#009688"));          // Teal
        setupChart(chestChart, chestEntries, "Chest", Color.parseColor("#3F51B5"));    // Indigo

    }

    private void setupChart(LineChart chart, List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.TRANSPARENT); // Hide point values
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curves
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))); // fill with semi-transparent color

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Chart styling
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.BLACK);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(color);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));

        // Y Axis (Left)
        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(color);

        // Y Axis (Right)
        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);

        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }


}
