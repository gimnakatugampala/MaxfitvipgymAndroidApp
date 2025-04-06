package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.maxfitvipgymapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class WorkoutActivity extends AppCompatActivity {

    private TextView workoutTitle, timerText, setInfoText;
    private ImageButton playPauseButton;
    private ImageView backgroundImage;

    private int currentWorkoutIndex = 0;
    private int timeLeft; // in seconds
    private boolean isRunning = true;
    private int currentSet = 1;
    private List<Integer> completedSets = new ArrayList<>();
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private MediaPlayer mediaPlayer;

    private Workout[] workouts = {
            new Workout("WEIGHT LIFTING", 10, "duration", 0, 0, "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg"),
            new Workout("HIT TRAINING", 10, "set", 3, 8, "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg?auto=compress&cs=tinysrgb&w=600&lazy=load"),
            new Workout("CARDIO BLAST", 15, "duration", 0, 0, "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg")
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

        ScrollView scrollContainer = findViewById(R.id.scrollContainer);

        // Listen for scroll events to detect upward scroll
        scrollContainer.setOnTouchListener(new View.OnTouchListener() {
            private float initialY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float deltaY = initialY - event.getY();
                        if (deltaY > 0) {  // Scrolling up
                            moveToNextWorkout();
                        }
                        break;
                }
                return false;
            }
        });

        setupWorkout();

        playPauseButton.setOnClickListener(v -> {
            isRunning = !isRunning;
            animatePlayPauseButton();

            if (isRunning) {
                startTimer();
                playPauseButton.setImageResource(R.drawable.pause);
            } else {
                stopTimer();
                playPauseButton.setImageResource(R.drawable.playbutton);
            }
        });
    }

    private void setupWorkout() {
        Workout workout = workouts[currentWorkoutIndex];
        workoutTitle.setText(workout.title);
        timeLeft = workout.time;
        currentSet = 1;
        completedSets.clear();

        Glide.with(this)
                .load(workout.imageUrl)
                .transition(withCrossFade())
                .into(backgroundImage);

        updateTimerText();

        if (workout.type.equals("set")) {
            setInfoText.setVisibility(View.VISIBLE);
            updateSetInfo();
        } else {
            setInfoText.setVisibility(View.GONE);
        }

        // Reset scroll position to top
        ScrollView scrollContainer = findViewById(R.id.scrollContainer);
        scrollContainer.post(() -> scrollContainer.fullScroll(View.FOCUS_UP));

        playPauseButton.setImageResource(R.drawable.pause);
        startTimer();
    }

    private void animatePlayPauseButton() {
        playPauseButton.animate()
                .scaleX(1.1f).scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> playPauseButton.animate().scaleX(1f).scaleY(1f).setDuration(100));
    }

    private void startTimer() {
        stopTimer();

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
        isRunning = true;
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        isRunning = false;
    }

    private void updateTimerText() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
    }

    private void handleTimerCompletion() {
        Workout current = workouts[currentWorkoutIndex];

        playSoundEffect();

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
            animateWorkoutTransition(() -> setupWorkout());
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

    private void playSoundEffect() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.ding);
        mediaPlayer.start();
    }

    private void animateWorkoutTransition(final Runnable updateContent) {
        final LinearLayout centerBlock = findViewById(R.id.centerBlock);
        final ScrollView scrollContainer = findViewById(R.id.scrollContainer);

        // Slide out to top (for existing content)
        TranslateAnimation slideOut = new TranslateAnimation(0, 0, 0, -centerBlock.getHeight());
        slideOut.setDuration(300);
        slideOut.setFillAfter(false);

        // Slide in from bottom (for new content)
        TranslateAnimation slideIn = new TranslateAnimation(0, 0, centerBlock.getHeight(), 0);
        slideIn.setDuration(300);
        slideIn.setFillAfter(true);

        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Update workout content after slide out completes
                updateContent.run();

                // Scroll to the bottom before sliding in new content
                scrollContainer.post(() -> scrollContainer.smoothScrollTo(0, scrollContainer.getHeight()));

                // Start slide-in animation for the new workout content
                centerBlock.startAnimation(slideIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Start slide-out animation for the current workout
        centerBlock.startAnimation(slideOut);
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();
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
