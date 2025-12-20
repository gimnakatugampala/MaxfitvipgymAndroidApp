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
import com.example.maxfitvipgymapp.Repository.ProgressRepository;
import com.example.maxfitvipgymapp.Utils.SessionManager;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InsightsFragment extends Fragment {

    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;
    private LineChart weightChart, bicepChart, hipChart, chestChart;
    private Button btnWeekly, btnMonthly, btnYearly;
    private ProgressRepository progressRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private int memberId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        sessionManager = new SessionManager(getContext());
        progressRepository = new ProgressRepository();
        executorService = Executors.newSingleThreadExecutor();
        memberId = sessionManager.getMemberId();

        // Chart and button references
        weightChart = view.findViewById(R.id.weight_chart);
        bicepChart = view.findViewById(R.id.bicep_chart);
        hipChart = view.findViewById(R.id.hip_chart);
        chestChart = view.findViewById(R.id.chest_chart);

        btnWeekly = view.findViewById(R.id.btn_weekly);
        btnMonthly = view.findViewById(R.id.btn_monthly);
        btnYearly = view.findViewById(R.id.btn_yearly);

        // Load initial chart data
        loadChartData("Weekly", 7);

        btnWeekly.setOnClickListener(v -> loadChartData("Weekly", 7));
        btnMonthly.setOnClickListener(v -> loadChartData("Monthly", 30));
        btnYearly.setOnClickListener(v -> loadChartData("Yearly", 365));

        // Date selector setup
        dateRecyclerView = view.findViewById(R.id.date_recycler_view);
        dateRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        setupDateSelector();

        // Stats grid
        loadLatestStats(view);

        return view;
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
            // Load data for selected date if needed
        });
        dateRecyclerView.setAdapter(dateAdapter);
        dateAdapter.setSelected(selectedIndex);
        dateRecyclerView.scrollToPosition(selectedIndex);
    }

    private void loadLatestStats(View view) {
        executorService.execute(() -> {
            Map<String, Double> stats = progressRepository.getLatestStats(memberId);

            getActivity().runOnUiThread(() -> {
                GridLayout statsGrid = view.findViewById(R.id.stats_grid);
                statsGrid.removeAllViews();

                int[] icons = {
                        R.drawable.ic_calories,
                        R.drawable.ic_weight,
                        R.drawable.strength,
                        R.drawable.ic_hip,
                        R.drawable.ic_chest,
                        R.drawable.heartrate
                };

                String[] labels = {
                        "Calories", "Weight (kg)", "Bicep Size (cm)",
                        "Hip Size (cm)", "Chest Size (cm)", "MAX H/R"
                };

                String[] values = {
                        "620.98", // Calories - from other source
                        String.format("%.2f", stats.getOrDefault("weight", 0.0)),
                        String.format("%.1f", stats.getOrDefault("bicep", 0.0)),
                        String.format("%.1f", stats.getOrDefault("hip", 0.0)),
                        String.format("%.1f", stats.getOrDefault("chest", 0.0)),
                        "180" // MAX HR - from other source
                };

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
            });
        });
    }

    private void loadChartData(String period, int days) {
        executorService.execute(() -> {
            // Get progress data
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

            getActivity().runOnUiThread(() -> {
                setupChart(weightChart, weightEntries, "Weight", Color.parseColor("#FFD300"));
                setupChart(bicepChart, bicepEntries, "Bicep", Color.parseColor("#FF5722"));
                setupChart(hipChart, hipEntries, "Hip", Color.parseColor("#009688"));
                setupChart(chestChart, chestEntries, "Chest", Color.parseColor("#3F51B5"));
            });
        });
    }

    private List<Entry> convertToEntries(List<Map<String, Object>> progress) {
        List<Entry> entries = new ArrayList<>();
        Collections.reverse(progress); // Reverse to show oldest first

        for (int i = 0; i < progress.size(); i++) {
            Map<String, Object> item = progress.get(i);
            double value = (double) item.get("value");
            entries.add(new Entry(i, (float) value));
        }

        return entries;
    }

    private void setupChart(LineChart chart, List<Entry> entries, String label, int color) {
        if (entries.isEmpty()) {
            // Show empty chart
            chart.clear();
            chart.setNoDataText("No data available");
            chart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.TRANSPARENT);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(100, Color.red(color),
                Color.green(color), Color.blue(color)));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.BLACK);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(color);
        xAxis.setGranularity(1f);

        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(color);

        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);

        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}