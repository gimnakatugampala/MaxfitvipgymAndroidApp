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

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private List<DateModel> days;

    public CalendarAdapter(List<DateModel> days) {
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DateModel model = days.get(position);

        holder.dayText.setText(model.getDate()); // Displays "1", "2", etc.

        // Just for reference - Ensure your CalendarAdapter has this logic in onBindViewHolder:
        if (model.isDisabled()) {
            // Future
            holder.dayBackground.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            holder.dayText.setTextColor(Color.parseColor("#444444"));
        } else if (model.isSelected()) {
            // STREAK HIT (Yellow)
            holder.dayBackground.setBackgroundResource(R.drawable.circle_yellow_bg);
            holder.dayBackground.setBackgroundTintList(null);
            holder.dayText.setTextColor(Color.BLACK);
        } else {
            // MISSED (Grey)
            holder.dayBackground.setBackgroundResource(R.drawable.circle_background);
            holder.dayBackground.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#333333")));
            holder.dayText.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }
}