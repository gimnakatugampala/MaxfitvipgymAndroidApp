package com.example.maxfitvipgymapp.Model;

import java.util.List;

public class Workout {
    private String name;
    private boolean isByDuration;
    private int duration; // in seconds
    private List<Integer> repsPerSet;
    private String imageUrl;
    private List<String> youtubeUrls;

    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet,
                   String imageUrl, List<String> youtubeUrls) {
        this.name = name;
        this.isByDuration = isByDuration;
        this.duration = duration;
        this.repsPerSet = repsPerSet;
        this.imageUrl = imageUrl;
        this.youtubeUrls = youtubeUrls;
    }

    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet) {
        this(name, isByDuration, duration, repsPerSet, null, null);
    }

    public String getName() {
        return name;
    }

    public boolean isByDuration() {
        return isByDuration;
    }

    public int getDuration() {
        return duration;
    }

    public List<Integer> getRepsPerSet() {
        return repsPerSet;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getYoutubeUrls() {
        return youtubeUrls;
    }

    // Optional setters
    public void setName(String name) {
        this.name = name;
    }

    public void setByDuration(boolean byDuration) {
        isByDuration = byDuration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setRepsPerSet(List<Integer> repsPerSet) {
        this.repsPerSet = repsPerSet;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setYoutubeUrls(List<String> youtubeUrls) {
        this.youtubeUrls = youtubeUrls;
    }
}
