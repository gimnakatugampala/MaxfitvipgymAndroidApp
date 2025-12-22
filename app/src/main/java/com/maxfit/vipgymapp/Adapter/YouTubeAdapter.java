package com.maxfit.vipgymapp.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxfit.vipgymapp.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.List;

public class YouTubeAdapter extends RecyclerView.Adapter<YouTubeAdapter.YouTubeViewHolder> {

    private List<String> videoIds;

    public YouTubeAdapter(List<String> videoIds) {
        this.videoIds = videoIds;
    }

    @NonNull
    @Override
    public YouTubeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(com.maxfit.vipgymapp.R.layout.item_youtube_video, parent, false);
        return new YouTubeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YouTubeViewHolder holder, int position) {
        String videoId = videoIds.get(position);
        holder.youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoIds.size();
    }

    static class YouTubeViewHolder extends RecyclerView.ViewHolder {

        YouTubePlayerView youtubePlayerView;

        public YouTubeViewHolder(View itemView) {
            super(itemView);
            youtubePlayerView = itemView.findViewById(R.id.youtubePlayerView);
            // Removed the enableAutomaticInitialization() method
        }
    }
}
