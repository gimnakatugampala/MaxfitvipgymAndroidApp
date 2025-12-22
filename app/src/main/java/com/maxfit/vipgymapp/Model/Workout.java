package com.maxfit.vipgymapp.Model;

import java.util.List;

public class Workout {
    private String name;
    private boolean isByDuration;
    private int duration; // in seconds
    private List<Integer> repsPerSet;
    private String imageUrl;
    private List<String> youtubeUrls;
    private int restSeconds = 60; // ✅ NEW: Default 60 seconds rest between sets

    // Main constructor
    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet,
                   String imageUrl, List<String> youtubeUrls) {
        this.name = name;
        this.isByDuration = isByDuration;
        this.duration = duration;
        this.repsPerSet = repsPerSet;
        this.imageUrl = imageUrl;
        this.youtubeUrls = youtubeUrls;
    }

    // ✅ NEW: Constructor with rest time
    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet,
                   String imageUrl, List<String> youtubeUrls, int restSeconds) {
        this(name, isByDuration, duration, repsPerSet, imageUrl, youtubeUrls);
        this.restSeconds = restSeconds;
    }

    // Legacy constructor (for backward compatibility)
    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet) {
        this(name, isByDuration, duration, repsPerSet, null, null);
    }

    // Getters
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

    // ✅ NEW: Get rest time between sets
    public int getRestSeconds() {
        return restSeconds;
    }

    // Setters
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

    // ✅ NEW: Set rest time between sets
    public void setRestSeconds(int restSeconds) {
        this.restSeconds = restSeconds;
    }

    // ✅ Helper method to check if workout has valid reps
    public boolean hasValidReps() {
        return repsPerSet != null && !repsPerSet.isEmpty();
    }

    @Override
    public String toString() {
        return "Workout{" +
                "name='" + name + '\'' +
                ", isByDuration=" + isByDuration +
                ", duration=" + duration +
                ", repsPerSet=" + repsPerSet +
                ", restSeconds=" + restSeconds +
                '}';
    }
}