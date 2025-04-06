package com.example.maxfitvipgymapp.Model;

public class DateModel {
    private String day;
    private String date;
    private boolean isSelected;
    private boolean isDisabled;

    public DateModel(String day, String date, boolean isSelected, boolean isDisabled) {
        this.day = day;
        this.date = date;
        this.isSelected = isSelected;
        this.isDisabled = isDisabled;
    }

    public String getDay() {
        return day;
    }

    public String getDate() {
        return date;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }
}
