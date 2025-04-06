package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.Model.Workout;
import com.example.maxfitvipgymapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import com.bumptech.glide.Glide;


public class WorkoutActivity extends AppCompatActivity {

    private TextView workoutTitle, timerText, setInfoText;
    private Button playPauseButton;
    private ImageView backgroundImage;
    private int currentWorkoutIndex = 0;
    private int timeLeft; // in seconds
    private boolean isRunning = true;
    private int currentSet = 1;
    private List<Integer> completedSets = new ArrayList<>();
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // Sample workouts
    private Workout[] workouts = {
            new Workout("Weight Lifting", 10, "duration", 0, 0, "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg"),
            new Workout("HIIT Training", 10, "set", 3, 8, "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg?auto=compress&cs=tinysrgb&w=600&lazy=load"),
            new Workout("Cardio Blast", 15, "duration", 0, 0, "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        workoutTitle = findViewById(R.id.workoutTitle);
        timerText = findViewById(R.id.timerText);
        playPauseButton = findViewById(R.id.playPauseButton);
        backgroundImage = findViewById(R.id.backgroundImage);
        setInfoText = findViewById(R.id.setInfoText);

        setupWorkout();

        playPauseButton.setOnClickListener(v -> {
            isRunning = !isRunning;
            if (isRunning) startTimer();
            else stopTimer();
        });
    }

    private void setupWorkout() {
        Workout workout = workouts[currentWorkoutIndex];
        workoutTitle.setText(workout.title);
        timeLeft = workout.time;

        Glide.with(this).load(workout.imageUrl).into(backgroundImage); // Use Glide for image

        updateTimerText();

        startTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    updateTimerText();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    handleTimerCompletion();
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateTimerText() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
    }

    private void handleTimerCompletion() {
        Workout current = workouts[currentWorkoutIndex];

        if (current.type.equals("set") && currentSet < current.sets) {
            completedSets.add(currentSet);
            currentSet++;
            timeLeft = current.time;
            updateSetInfo();
            startTimer();
        } else {
            moveToNextWorkout();
        }
    }

    private void moveToNextWorkout() {
        currentWorkoutIndex++;
        if (currentWorkoutIndex < workouts.length) {
            currentSet = 1;
            completedSets.clear();
            setupWorkout();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Good Job!")
                    .setMessage("You have completed all workouts.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
        }
    }

    private void updateSetInfo() {
        Workout current = workouts[currentWorkoutIndex];
        if (!current.type.equals("set")) return;

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= current.sets; i++) {
            if (completedSets.contains(i)) builder.append("✅ ");
            else builder.append(i).append(" ");
        }
        setInfoText.setText(builder.toString().trim());
    }

    static class Workout {
        String title;
        int time;
        String type;
        int sets;
        int reps;
        String imageUrl;

        public Workout(String title, int time, String type, int sets, int reps, String imageUrl) {
            this.title = title;
            this.time = time;
            this.type = type;
            this.sets = sets;
            this.reps = reps;
            this.imageUrl = imageUrl;
        }
    }
}
