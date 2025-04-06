package com.example.maxfitvipgymapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maxfitvipgymapp.Adapter.DateAdapter;
import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.gridlayout.widget.GridLayout; // ✅ Correct import

public class InsightsFragment extends Fragment {

    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        // Date selector setup
        dateRecyclerView = view.findViewById(R.id.date_recycler_view);
        dateRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        List<DateModel> dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Move to yesterday

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");

        for (int i = 0; i < 8; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());

            boolean isToday = i == 1;       // Today is index 1
            boolean isDisabled = i == 0;    // Yesterday is disabled

            dateList.add(new DateModel(day, date, isToday, isDisabled));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        dateAdapter = new DateAdapter(dateList, position -> dateAdapter.setSelected(position));
        dateRecyclerView.setAdapter(dateAdapter);

        // Stats Grid setup
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

        // Inside onCreateView(), below "statsGrid" definition:
        for (int i = 0; i < values.length; i++) {
            View statItem = LayoutInflater.from(getContext()).inflate(R.layout.item_stat, statsGrid, false);

            // Set the icon, value, and label
            ((ImageView) statItem.findViewById(R.id.stat_icon)).setImageResource(icons[i]);
            ((TextView) statItem.findViewById(R.id.stat_value)).setText(values[i]);
            ((TextView) statItem.findViewById(R.id.stat_label)).setText(labels[i]);

            // Set layout params to allow 2 cards per row (or 3 if columnCount = 3)
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Take up equal width with weight
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            statItem.setLayoutParams(params);

            statsGrid.addView(statItem);
        }


        return view;
    }
}
