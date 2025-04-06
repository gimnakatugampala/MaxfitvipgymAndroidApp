package com.example.maxfitvipgymapp.Activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.Model.Workout;
import com.example.maxfitvipgymapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkoutActivity extends AppCompatActivity {

    private List<Workout> workoutList;
    private int currentWorkoutIndex = 0;
    private CountDownTimer countDownTimer;
    private TextView timerText;
    private Button startButton;

    private int currentSet = 0;
    private int currentRep = 0;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        timerText = findViewById(R.id.timerText);
        startButton = findViewById(R.id.startWorkoutButton);

        workoutList = getDummyWorkouts(); // Load from intent or DB

        startButton.setOnClickListener(v -> {
            if (!isRunning) {
                runWorkout(workoutList.get(currentWorkoutIndex));
            }
        });
    }

    private void runWorkout(Workout workout) {
        isRunning = true;

        if (workout.isByDuration()) {
            runDurationWorkout(workout);
        } else {
            runSetWorkout(workout);
        }
    }

    private void runDurationWorkout(Workout workout) {
        countDownTimer = new CountDownTimer(workout.getDuration() * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                moveToNextWorkout();
            }
        }.start();
    }

    private void runSetWorkout(Workout workout) {
        List<Integer> sets = workout.getRepsPerSet();

        if (currentSet >= sets.size()) {
            moveToNextWorkout();
            return;
        }

        int reps = sets.get(currentSet);
        currentRep = 0;

        // simulate reps with delay (for testing)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentRep++;
                timerText.setText("Set " + (currentSet + 1) + " - Rep " + currentRep);

                if (currentRep < reps) {
                    new Handler().postDelayed(this, 1000); // 1 sec delay between reps
                } else {
                    currentSet++;
                    runSetWorkout(workout); // Move to next set
                }
            }
        }, 1000);
    }

    private void moveToNextWorkout() {
        currentWorkoutIndex++;
        currentSet = 0;

        if (currentWorkoutIndex < workoutList.size()) {
            runWorkout(workoutList.get(currentWorkoutIndex));
        } else {
            timerText.setText("Workout Completed!");
            isRunning = false;
        }
    }

    private List<Workout> getDummyWorkouts() {
        List<Workout> list = new ArrayList<>();

        list.add(new Workout("Plank", true, 10, null));
        list.add(new Workout("Push-ups", false, 0, Arrays.asList(8, 8, 8)));

        return list;
    }
}
