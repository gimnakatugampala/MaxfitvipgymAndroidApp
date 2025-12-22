package com.maxfit.vipgymapp.Adapter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.maxfit.vipgymapp.Model.DateModel;
import com.maxfit.vipgymapp.R;
import android.content.res.ColorStateList;


import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {

    private List<DateModel> dateList;
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(int position);
    }

    public DateAdapter(List<DateModel> dateList, OnDateClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayText, dateText;
        CardView cardView;

        public ViewHolder(View view) {
            super(view);
            dayText = view.findViewById(R.id.dayText);
            dateText = view.findViewById(R.id.dateText);
            cardView = view.findViewById(R.id.cardView);

            view.setOnClickListener(v -> {
                listener.onDateClick(getAdapterPosition());
            });
        }
    }

    @Override
    public DateAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DateModel model = dateList.get(position);

        holder.dayText.setText(model.getDay());
        holder.dateText.setText(model.getDate());

        if (model.isDisabled()) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1A1A1A"));
            holder.dayText.setTextColor(Color.parseColor("#555555"));
            holder.dateText.setTextColor(Color.parseColor("#555555"));
            holder.dateText.setBackground(null); // Reset background
        } else if (model.isSelected()) {
            holder.cardView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD300")));
            holder.dateText.setBackgroundResource(R.drawable.day_selected_background);
            holder.dayText.setTextColor(Color.WHITE);
            holder.dateText.setTextColor(Color.BLACK);
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1A1A1A")); // Default style
            holder.cardView.setBackgroundTintList(null); // Reset tint
            holder.dayText.setTextColor(Color.WHITE);
            holder.dateText.setTextColor(Color.WHITE);
            holder.dateText.setBackground(null); // Reset background
        }
    }


    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public void setSelected(int position) {
        if (dateList.get(position).isDisabled()) return; // 🔒 skip if disabled

        for (int i = 0; i < dateList.size(); i++) {
            dateList.get(i).setSelected(i == position);
        }
        notifyDataSetChanged();
    }


}
