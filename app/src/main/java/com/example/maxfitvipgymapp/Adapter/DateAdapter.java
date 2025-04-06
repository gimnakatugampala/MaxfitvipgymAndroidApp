package com.example.maxfitvipgymapp.Adapter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maxfitvipgymapp.Model.DateModel;
import com.example.maxfitvipgymapp.R;

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
        holder.dayText.setText(model.day);
        holder.dateText.setText(model.date);

        if (model.isSelected) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFD300"));
            holder.dateText.setTextColor(Color.BLACK);
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1A1A1A"));
            holder.dateText.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public void setSelected(int position) {
        for (int i = 0; i < dateList.size(); i++) {
            dateList.get(i).isSelected = i == position;
        }
        notifyDataSetChanged();
    }
}
