package com.example.maxfitvipgymapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maxfitvipgymapp.Model.MonthModel;
import com.example.maxfitvipgymapp.R;
import java.util.List;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder> {
    private List<MonthModel> months;
    private Context context;

    public CalendarMonthAdapter(Context context, List<MonthModel> months) {
        this.context = context;
        this.months = months;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView monthTitle;
        RecyclerView daysRecyclerView;

        public ViewHolder(View view) {
            super(view);
            monthTitle = view.findViewById(R.id.monthTitle);
            daysRecyclerView = view.findViewById(R.id.daysRecyclerView);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MonthModel month = months.get(position);
        holder.monthTitle.setText(month.getMonthName());

        // Setup the inner Grid (7 columns for days of week)
        CalendarDayAdapter dayAdapter = new CalendarDayAdapter(month.getDays());
        holder.daysRecyclerView.setLayoutManager(new GridLayoutManager(context, 7));
        holder.daysRecyclerView.setAdapter(dayAdapter);
    }

    @Override
    public int getItemCount() { return months.size(); }
}