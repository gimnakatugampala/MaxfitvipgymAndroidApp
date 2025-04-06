package com.example.maxfitvipgymapp.Model;

import java.util.List;

public class Workout {
    private String name;
    private boolean isByDuration;
    private int duration; // in seconds
    private List<Integer> repsPerSet;

    public Workout(String name, boolean isByDuration, int duration, List<Integer> repsPerSet) {
        this.name = name;
        this.isByDuration = isByDuration;
        this.duration = duration;
        this.repsPerSet = repsPerSet;
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

    public String getName() {
        return name;
    }

    // Optionally, add setters if needed
}
