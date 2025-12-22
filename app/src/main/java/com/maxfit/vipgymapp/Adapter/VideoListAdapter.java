package com.maxfit.vipgymapp.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {

    private List<String> videoIds;
    private OnVideoClickListener onVideoClickListener;

    public interface OnVideoClickListener {
        void onVideoClick(String videoId);
    }

    public VideoListAdapter(List<String> videoIds, OnVideoClickListener onVideoClickListener) {
        this.videoIds = videoIds;
        this.onVideoClickListener = onVideoClickListener;
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VideoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        String videoId = videoIds.get(position);
        holder.videoTitle.setText("Video " + (position + 1));  // You can customize the title here

        holder.itemView.setOnClickListener(v -> onVideoClickListener.onVideoClick(videoId));
    }

    @Override
    public int getItemCount() {
        return videoIds.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        public TextView videoTitle;

        public VideoViewHolder(View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(android.R.id.text1);
        }
    }
}
