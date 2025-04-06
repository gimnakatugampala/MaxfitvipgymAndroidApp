package com.example.maxfitvipgymapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class InsightsFragment extends Fragment {

    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        dateRecyclerView = view.findViewById(R.id.date_recycler_view);
        dateRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        List<DateModel> dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

// Move to yesterday first
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd");

        int todayIndex = 1; // Now today is at index 1
        for (int i = 0; i < 8; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());
            boolean isToday = i == 1; // index 1 is today

            dateList.add(new DateModel(day, date, isToday));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }


        dateAdapter = new DateAdapter(dateList, position -> {
            dateAdapter.setSelected(position);
        });

        dateRecyclerView.setAdapter(dateAdapter);
        return view;
    }
}
