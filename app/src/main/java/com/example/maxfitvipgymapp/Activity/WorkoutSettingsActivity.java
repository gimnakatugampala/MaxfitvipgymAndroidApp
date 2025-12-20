package com.example.maxfitvipgymapp.Activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.maxfitvipgymapp.R;

public class WorkoutSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WorkoutSettings";
    private static final String KEY_VOICE_ENABLED = "voiceGuidanceEnabled";
    private static final String KEY_SPEECH_RATE = "speechRate";
    private static final String KEY_SPEECH_PITCH = "speechPitch";
    private static final String KEY_COUNTDOWN_START = "countdownStart";
    private static final String KEY_MUSIC_DUCK_PERCENT = "musicDuckPercent";
    private static final String KEY_ANNOUNCE_EXERCISE = "announceExercise";
    private static final String KEY_ANNOUNCE_REPS = "announceReps";
    private static final String KEY_ANNOUNCE_REST = "announceRest";

    private SharedPreferences prefs;
    private Switch switchVoiceGuidance;
    private Switch switchAnnounceExercise;
    private Switch switchAnnounceReps;
    private Switch switchAnnounceRest;
    private SeekBar seekSpeechRate;
    private SeekBar seekSpeechPitch;
    private SeekBar seekCountdownStart;
    private SeekBar seekMusicDuck;
    private TextView tvSpeechRateValue;
    private TextView tvSpeechPitchValue;
    private TextView tvCountdownValue;
    private TextView tvMusicDuckValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Switches
        switchVoiceGuidance = findViewById(R.id.switchVoiceGuidance);
        switchAnnounceExercise = findViewById(R.id.switchAnnounceExercise);
        switchAnnounceReps = findViewById(R.id.switchAnnounceReps);
        switchAnnounceRest = findViewById(R.id.switchAnnounceRest);

        // SeekBars
        seekSpeechRate = findViewById(R.id.seekSpeechRate);
        seekSpeechPitch = findViewById(R.id.seekSpeechPitch);
        seekCountdownStart = findViewById(R.id.seekCountdownStart);
        seekMusicDuck = findViewById(R.id.seekMusicDuck);

        // TextViews for values
        tvSpeechRateValue = findViewById(R.id.tvSpeechRateValue);
        tvSpeechPitchValue = findViewById(R.id.tvSpeechPitchValue);
        tvCountdownValue = findViewById(R.id.tvCountdownValue);
        tvMusicDuckValue = findViewById(R.id.tvMusicDuckValue);

        // Configure SeekBars
        seekSpeechRate.setMax(150); // 0.5 to 2.0 (scaled by 100)
        seekSpeechPitch.setMax(150); // 0.5 to 2.0 (scaled by 100)
        seekCountdownStart.setMax(10); // 5 to 15 seconds
        seekMusicDuck.setMax(30); // 20% to 50% (scaled by 100)
    }

    private void loadSettings() {
        // Load switch states
        switchVoiceGuidance.setChecked(prefs.getBoolean(KEY_VOICE_ENABLED, true));
        switchAnnounceExercise.setChecked(prefs.getBoolean(KEY_ANNOUNCE_EXERCISE, true));
        switchAnnounceReps.setChecked(prefs.getBoolean(KEY_ANNOUNCE_REPS, true));
        switchAnnounceRest.setChecked(prefs.getBoolean(KEY_ANNOUNCE_REST, true));

        // Load seekbar values
        float speechRate = prefs.getFloat(KEY_SPEECH_RATE, 0.9f);
        seekSpeechRate.setProgress((int)((speechRate - 0.5f) * 100));
        tvSpeechRateValue.setText(String.format("%.1fx", speechRate));

        float speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f);
        seekSpeechPitch.setProgress((int)((speechPitch - 0.5f) * 100));
        tvSpeechPitchValue.setText(String.format("%.1fx", speechPitch));

        int countdownStart = prefs.getInt(KEY_COUNTDOWN_START, 10);
        seekCountdownStart.setProgress(countdownStart - 5);
        tvCountdownValue.setText(countdownStart + "s");

        float musicDuck = prefs.getFloat(KEY_MUSIC_DUCK_PERCENT, 0.3f);
        seekMusicDuck.setProgress((int)((musicDuck - 0.2f) * 100));
        tvMusicDuckValue.setText((int)(musicDuck * 100) + "%");

        // Enable/disable dependent switches
        updateDependentSwitches(switchVoiceGuidance.isChecked());
    }

    private void setupListeners() {
        // Master voice guidance switch
        switchVoiceGuidance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_VOICE_ENABLED, isChecked).apply();
            updateDependentSwitches(isChecked);
        });

        // Announcement switches
        switchAnnounceExercise.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_ANNOUNCE_EXERCISE, isChecked).apply()
        );

        switchAnnounceReps.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_ANNOUNCE_REPS, isChecked).apply()
        );

        switchAnnounceRest.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_ANNOUNCE_REST, isChecked).apply()
        );

        // Speech rate seekbar
        seekSpeechRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 0.5f + (progress / 100.0f);
                tvSpeechRateValue.setText(String.format("%.1fx", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float value = 0.5f + (seekBar.getProgress() / 100.0f);
                prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply();
            }
        });

        // Speech pitch seekbar
        seekSpeechPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 0.5f + (progress / 100.0f);
                tvSpeechPitchValue.setText(String.format("%.1fx", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float value = 0.5f + (seekBar.getProgress() / 100.0f);
                prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply();
            }
        });

        // Countdown start seekbar
        seekCountdownStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 5;
                tvCountdownValue.setText(value + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress() + 5;
                prefs.edit().putInt(KEY_COUNTDOWN_START, value).apply();
            }
        });

        // Music duck seekbar
        seekMusicDuck.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 0.2f + (progress / 100.0f);
                tvMusicDuckValue.setText((int)(value * 100) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float value = 0.2f + (seekBar.getProgress() / 100.0f);
                prefs.edit().putFloat(KEY_MUSIC_DUCK_PERCENT, value).apply();
            }
        });
    }

    private void updateDependentSwitches(boolean enabled) {
        switchAnnounceExercise.setEnabled(enabled);
        switchAnnounceReps.setEnabled(enabled);
        switchAnnounceRest.setEnabled(enabled);
        seekSpeechRate.setEnabled(enabled);
        seekSpeechPitch.setEnabled(enabled);
        seekCountdownStart.setEnabled(enabled);
        seekMusicDuck.setEnabled(enabled);
    }

    // Static helper methods to access settings from WorkoutActivity
    public static boolean isVoiceGuidanceEnabled(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_VOICE_ENABLED, true);
    }

    public static float getSpeechRate(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_SPEECH_RATE, 0.9f);
    }

    public static float getSpeechPitch(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_SPEECH_PITCH, 1.0f);
    }

    public static int getCountdownStart(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_COUNTDOWN_START, 10);
    }

    public static float getMusicDuckPercent(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_MUSIC_DUCK_PERCENT, 0.3f);
    }

    public static boolean shouldAnnounceExercise(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ANNOUNCE_EXERCISE, true);
    }

    public static boolean shouldAnnounceReps(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ANNOUNCE_REPS, true);
    }

    public static boolean shouldAnnounceRest(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ANNOUNCE_REST, true);
    }
}