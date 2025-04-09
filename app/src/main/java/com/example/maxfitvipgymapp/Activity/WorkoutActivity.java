package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.maxfitvipgymapp.Adapter.YouTubeAdapter;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Service.WorkoutForegroundService;
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
    private ImageView showVideoButton;

    private ImageView backFromWorkout;

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

    // Updated to include multiple YouTube video IDs
    private Workout[] workouts = {
            new Workout("WEIGHT LIFTING", true, 600, null, "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg",
                    Arrays.asList("dQw4w9WgXcQ", "kXYiU_JCYtU")), // Multiple YouTube video IDs
            new Workout("HIT TRAINING", false, 60, Arrays.asList(8, 8, 8), "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg",
                    Arrays.asList("9bZkp7q19f0")), // Multiple YouTube video IDs
            new Workout("CARDIO BLAST", true, 900, null, "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg",
                    Arrays.asList("9bZkp7q19f0", "dQw4w9WgXcQ")) // Multiple YouTube video IDs
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // Register broadcast receiver to update UI when timer changes



        // Initialize the views
        workoutTitle = findViewById(R.id.workoutTitle);
        timerText = findViewById(R.id.timerText);
        playPauseButton = findViewById(R.id.playPauseButton);
        backgroundImage = findViewById(R.id.backgroundImage);
        setInfoText = findViewById(R.id.setInfoText);
        showVideoButton = findViewById(R.id.showVideoButton);

        backFromWorkout = findViewById(R.id.backFromWorkout);

        youtubeModal = findViewById(R.id.youtubeModal);
        youtubePlayerView = findViewById(R.id.youtubePlayerView); // Initialize after setContentView
        closeYoutubeButton = findViewById(R.id.closeYoutubeButton);

        // Add observer for lifecycle
        getLifecycle().addObserver(youtubePlayerView);

        // Load sample video (using the first video ID of the list)
        showVideoButton.setOnClickListener(v -> showYouTubeVideo(workouts[currentWorkoutIndex].getYoutubeUrls().get(0))); // using the first video ID in the list

        closeYoutubeButton.setOnClickListener(v -> youtubeModal.setVisibility(View.GONE));

        // Load sample video (using the first video ID of the list)
        // Load sample video (using the first video ID of the list)
        showVideoButton.setOnClickListener(v -> {
            // Pause the timer when the demo video modal is shown
            stopTimer();  // Stop the timer when the video is shown
            playPauseButton.setImageResource(R.drawable.playbutton);  // Change button to "Pause" when video is shown
            showYouTubeVideo(workouts[currentWorkoutIndex].getYoutubeUrls().get(0)); // using the first video ID in the list
        });

// Close the YouTube modal and resume the timer
        closeYoutubeButton.setOnClickListener(v -> {
            // Close the modal
            youtubeModal.setVisibility(View.GONE);

            // Reset the playPauseButton to play when modal is closed
            playPauseButton.setImageResource(R.drawable.playbutton);  // Change button to "Play" when video is closed

            // Only restart the timer if it's not already running
            if (!isRunning) {
                startTimer();  // Start or resume the timer if it's not running
            }
        });

        backFromWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a confirmation dialog
                new AlertDialog.Builder(WorkoutActivity.this)
                        .setTitle("Stop Workout?")
                        .setMessage("Are you sure you want to stop your workout and go back to the home screen?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Stop the timer
                                stopTimer();

                                // Corrected Intent creation (removed OnClickListener and fixed context)
                                Intent serviceIntent = new Intent(WorkoutActivity.this, WorkoutForegroundService.class);
                                serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, "Cardio");
                                serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, 300); // 5 min
                                ContextCompat.startForegroundService(WorkoutActivity.this, serviceIntent);
                            }
                        })
                        .setNegativeButton("No", null)  // Do nothing if "No" is clicked
                        .show();  // Display the dialog
            }
        });




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


    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int timeLeft = intent.getIntExtra("timeLeft", 0);
            // Update the app's timer UI
            updateTimerText(timeLeft);  // Make sure to use the same logic as your app's timer
        }
    };

    private void updateTimerText(int timeLeft) {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
    }



    private void setupWorkout() {
        Workout workout = workouts[currentWorkoutIndex];
        workoutTitle.setText(workout.getTitle());
        timeLeft = workout.getTime();
        currentSet = 1;
        completedSets.clear();

//        Notification
        Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, workout.getTitle());
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, workout.getTime());
        startService(serviceIntent);


        Glide.with(this)
                .load(workout.getImageUrl())
                .transition(withCrossFade())
                .into(backgroundImage);

        updateTimerText();

        if (!workout.isDurationBased() && workout.getRepsPerSet() != null) {
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

        // Start both the app timer and the service timer
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    updateTimerText();
                    updateServiceTimer(); // This keeps the notification in sync
                    timerHandler.postDelayed(this, 1000);
                } else {
                    handleTimerCompletion();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
        isRunning = true;
    }

    private void updateServiceTimer() {
        Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, timeLeft);
        startService(serviceIntent);
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

        if (!current.isDurationBased() && currentSet < current.getRepsPerSet().size()) {
            completedSets.add(currentSet);
            currentSet++;
            timeLeft = current.getTime();
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

        stopService(new Intent(this, WorkoutForegroundService.class));

    }

    private void updateSetInfo() {
        Workout current = workouts[currentWorkoutIndex];
        if (current.getRepsPerSet() == null) return;

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= current.getRepsPerSet().size(); i++) {
            if (completedSets.contains(i)) {
                builder.append("✅ ");
            } else {
                builder.append("Set ").append(i).append(": ").append(current.getRepsPerSet().get(i - 1)).append(" reps\n");
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
        youtubeModal.setVisibility(View.VISIBLE);  // Show the full-screen YouTube modal (popup)

        List<String> videoList = workouts[currentWorkoutIndex].getYoutubeUrls();

        // Set up ViewPager2 with an adapter
        YouTubeAdapter adapter = new YouTubeAdapter(videoList);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);

        // Close YouTube modal on click
        closeYoutubeButton.setOnClickListener(v -> youtubeModal.setVisibility(View.GONE));  // Hide the modal
    }








    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();

        // Unregister the receiver when the activity is destroyed
        unregisterReceiver(timerUpdateReceiver);
    }

    static class Workout {
        private String title;
        private boolean isDurationBased;
        private int time;
        private List<Integer> repsPerSet;
        private String imageUrl;
        private List<String> youtubeUrls;  // New field for multiple YouTube video IDs

        public Workout(String title, boolean isDurationBased, int time, List<Integer> repsPerSet, String imageUrl, List<String> youtubeUrls) {
            this.title = title;
            this.isDurationBased = isDurationBased;
            this.time = time;
            this.repsPerSet = repsPerSet;
            this.imageUrl = imageUrl;
            this.youtubeUrls = youtubeUrls;
        }

        public String getTitle() {
            return title;
        }

        public boolean isDurationBased() {
            return isDurationBased;
        }

        public int getTime() {
            return time;
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
    }
}
