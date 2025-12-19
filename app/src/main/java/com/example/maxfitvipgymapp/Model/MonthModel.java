package com.example.maxfitvipgymapp.Model;

import java.util.List;

public class MonthModel {
    private String monthName;
    private List<DateModel> days;

    public MonthModel(String monthName, List<DateModel> days) {
        this.monthName = monthName;
        this.days = days;
    }

    public String getMonthName() { return monthName; }
    public List<DateModel> getDays() { return days; }
}