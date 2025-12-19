package com.example.maxfitvipgymapp.Adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.R;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {
    private List<DateModel> days;

    public CalendarDayAdapter(List<DateModel> days) {
        this.days = days;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        View dayBackground;
        public ViewHolder(View view) {
            super(view);
            dayText = view.findViewById(R.id.dayText);
            dayBackground = view.findViewById(R.id.dayBackground);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DateModel model = days.get(position);
        holder.dayText.setText(model.getDate());

        if (model.getDate().isEmpty()) {
            // Empty spacer for start of month
            holder.dayBackground.setVisibility(View.INVISIBLE);
            holder.dayText.setVisibility(View.INVISIBLE);
            return;
        }

        holder.dayBackground.setVisibility(View.VISIBLE);
        holder.dayText.setVisibility(View.VISIBLE);

        if (model.isDisabled()) {
            // Future date
            holder.dayBackground.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#222222")));
            holder.dayText.setTextColor(Color.DKGRAY);
        } else if (model.isSelected()) {
            // STREAK! (Yellow/Gold)
            holder.dayBackground.setBackgroundResource(R.drawable.circle_yellow_bg);
            holder.dayBackground.setBackgroundTintList(null);
            holder.dayText.setTextColor(Color.BLACK);
        } else {
            // Missed (Dark Grey)
            holder.dayBackground.setBackgroundResource(R.drawable.circle_background);
            holder.dayBackground.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#444444")));
            holder.dayText.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return days.size(); }
}