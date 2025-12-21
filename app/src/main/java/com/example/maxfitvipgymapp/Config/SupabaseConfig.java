package com.example.maxfitvipgymapp.Config;

public class SupabaseConfig {
    // Replace these with your actual Supabase credentials
    public static final String SUPABASE_URL = "https://atdfzrqciomceoshlujd.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF0ZGZ6cnFjaW9tY2Vvc2hsdWpkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU5OTUyMTEsImV4cCI6MjA4MTU3MTIxMX0.w6KuEellpzcutAMmbox8vZ_eritihZc63TdoH2MUy4k";

    // API Endpoints
    public static final String BASE_URL = SUPABASE_URL + "/rest/v1/";

    // Table names
    public static final String TABLE_MEMBERS = "members";
    public static final String TABLE_ATTENDANCE = "attendence";
    public static final String TABLE_WEIGHT_PROGRESS = "weight_progress";
    public static final String TABLE_BICEP_PROGRESS = "bicep_progress";
    public static final String TABLE_CHEST_PROGRESS = "chest_progress";
    public static final String TABLE_HIP_SIZE_PROGRESS = "hip_size_progress";
    public static final String TABLE_WORKOUT = "workout";
    public static final String TABLE_MEMBER_WORKOUT_SCHEDULE = "member_workout_schedule";
    public static final String TABLE_MEMBER_WORKOUT_SCHEDULE_DETAILS = "member_workout_schedule_details";
    public static final String TABLE_WORKOUT_SCHEDULE_DETAILS = "workout_schedule_details";
    public static final String TABLE_WORK_SCHEDULE = "work_schedule";
    public static final String TABLE_DAY = "day";
    public static final String TABLE_MAX_HR = "max_hr_inch";
    public static final String TABLE_PAYMENT_MEMBERS = "payment_members";

    public static final String TABLE_WORKOUT_COMPLETIONS = "workout_completions";
}