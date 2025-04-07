package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.maxfitvipgymapp.R;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class WorkoutActivity extends AppCompatActivity {

    private TextView workoutTitle, timerText, setInfoText;
    private ImageButton playPauseButton;
    private ImageView backgroundImage;
    private Button showVideoButton;

    private FrameLayout youtubeModal;
    private YouTubePlayerView youtubePlayerView;
    private ImageButton closeYoutubeButton;

    private int currentWorkoutIndex = 0;
    private int timeLeft;
    private boolean isRunning = true;
    private int currentSet = 1;
    private List<Integer> completedSets = new ArrayList<>();
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private MediaPlayer mediaPlayer;
    private boolean isTransitioning = false;
    private GestureDetector gestureDetector;

    private Workout[] workouts = {
            new Workout("WEIGHT LIFTING", true, 600, null, "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg"),
            new Workout("HIT TRAINING", false, 60, Arrays.asList(8, 8, 8), "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg"),
            new Workout("CARDIO BLAST", true, 900, null, "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg")
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
        showVideoButton = findViewById(R.id.showVideoButton);

        youtubeModal = findViewById(R.id.youtubeModal);
        youtubePlayerView = findViewById(R.id.youtubePlayerView);
        closeYoutubeButton = findViewById(R.id.closeYoutubeButton);

        getLifecycle().addObserver(youtubePlayerView); // Required for proper lifecycle

        // Load sample video
        showVideoButton.setOnClickListener(v -> showYouTubeVideo("dQw4w9WgXcQ")); // replace with actual ID

        closeYoutubeButton.setOnClickListener(v -> youtubeModal.setVisibility(View.GONE));

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (distanceY > 50 && !isTransitioning) {
                    moveToNextWorkout();
                    return true;
                }
                return false;
            }
        });

        ScrollView scrollContainer = findViewById(R.id.scrollContainer);
        scrollContainer.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollContainer.getScrollY();
            if (scrollY > 300 && !isTransitioning) {
                moveToNextWorkout();
            }
        });

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

        setupWorkout();
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

        if (!workout.isDurationBased && workout.repsPerSet != null) {
            setInfoText.setVisibility(View.VISIBLE);
            updateSetInfo();
        } else {
            setInfoText.setVisibility(View.GONE);
        }

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

        if (!current.isDurationBased && currentSet < current.repsPerSet.size()) {
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
        if (isTransitioning) return;

        isTransitioning = true;
        currentWorkoutIndex++;
        if (currentWorkoutIndex < workouts.length) {
            animateWorkoutTransition(() -> {
                setupWorkout();
                isTransitioning = false;
            });
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Good Job!")
                    .setMessage("You have completed all workouts.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
            isTransitioning = false;
        }
    }

    private void updateSetInfo() {
        Workout current = workouts[currentWorkoutIndex];
        if (current.repsPerSet == null) return;

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= current.repsPerSet.size(); i++) {
            if (completedSets.contains(i)) {
                builder.append("✅ ");
            } else {
                builder.append("Set ").append(i).append(": ").append(current.repsPerSet.get(i - 1)).append(" reps\n");
            }
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

        TranslateAnimation slideOut = new TranslateAnimation(0, 0, 0, -centerBlock.getHeight());
        slideOut.setDuration(300);
        slideOut.setFillAfter(false);

        TranslateAnimation slideIn = new TranslateAnimation(0, 0, centerBlock.getHeight(), 0);
        slideIn.setDuration(300);
        slideIn.setFillAfter(true);

        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                updateContent.run();
                scrollContainer.post(() -> scrollContainer.smoothScrollTo(0, scrollContainer.getHeight()));
                centerBlock.startAnimation(slideIn);
            }
        });

        centerBlock.startAnimation(slideOut);
    }

    private void showYouTubeVideo(String videoId) {
        youtubeModal.setVisibility(View.VISIBLE);

        youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0);
            }
        });
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
        boolean isDurationBased;
        int time;
        List<Integer> repsPerSet;
        String imageUrl;

        public Workout(String title, boolean isDurationBased, int time, List<Integer> repsPerSet, String imageUrl) {
            this.title = title;
            this.isDurationBased = isDurationBased;
            this.time = time;
            this.repsPerSet = repsPerSet;
            this.imageUrl = imageUrl;
        }
    }
}
