package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.util.Log;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
            new Workout("WEIGHT LIFTING", true, 10, null, "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg",
                    Arrays.asList("dQw4w9WgXcQ", "kXYiU_JCYtU")), // Multiple YouTube video IDs
            new Workout("HIT TRAINING", false, 10, Arrays.asList(8, 8, 8), "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg",
                    Arrays.asList("9bZkp7q19f0")), // Multiple YouTube video IDs
            new Workout("CARDIO BLAST", true, 10, null, "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg",
                    Arrays.asList("9bZkp7q19f0", "dQw4w9WgXcQ")) // Multiple YouTube video IDs
    };


    private boolean isReceiverRegistered = false;

    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int timeLeft = intent.getIntExtra("timeLeft", 0);
            updateTimerText(timeLeft);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true; // Make sure we are returning true if gesture is detected
        }
        return super.onTouchEvent(event);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);




        // Register broadcast receiver to update UI when timer changes
        // Register the receiver with LocalBroadcastManager
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, new IntentFilter("TIMER_UPDATE"));
            isReceiverRegistered = true;
        }





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

        backFromWorkout.setOnClickListener(v -> {
            new AlertDialog.Builder(WorkoutActivity.this)
                    .setTitle("Stop Workout?")
                    .setMessage("Are you sure you want to stop your workout and go back to the home screen?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        stopTimer();
                        stopService(new Intent(WorkoutActivity.this, WorkoutForegroundService.class));
                        sendWorkoutCancelledNotification(workouts[currentWorkoutIndex].getTitle());
                        Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                        intent.putExtra("navigateTo", "home");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });





        // Gesture detection for swipe to move to next workout
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

        scrollContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

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


    @Override
    protected void onStart() {
        super.onStart();
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, new IntentFilter("TIMER_UPDATE"));
            isReceiverRegistered = true;
        }
    }

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
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_IS_RESTING, false);  // Workout state
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
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, workouts[currentWorkoutIndex].getTitle()); // Add this line
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

        // If the workout is not duration-based and we haven't completed all sets
        if (!current.isDurationBased() && currentSet < current.getRepsPerSet().size()) {
            completedSets.add(currentSet);

            // Start rest timer for 1 minute before next set
            timeLeft = 60; // 1 minute rest time
            timerText.setText("Resting...");
            setInfoText.setText("Rest before next set");

            // Update the service to reflect rest period
            Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_IS_RESTING, true); // Indicate resting state
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, timeLeft);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, current.getTitle());
            startService(serviceIntent);

            // Start the countdown for the rest period
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (timeLeft > 0) {
                        updateTimerText(); // Show actual countdown
                        timeLeft--;
                        updateServiceTimer(); // Update notification timer
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        // Start next set after rest
                        currentSet++;
                        timeLeft = current.getTime(); // Reset time for the next set
                        updateSetInfo();
                        startTimer(); // Resume the workout
                    }
                }
            };

            timerHandler.postDelayed(timerRunnable, 1000);
            isRunning = true;
        } else {
            // All sets done OR duration-based workout finished
            sendWorkoutCompletedNotification();
            moveToNextWorkout();
        }
    }




    private void moveToNextWorkout() {
        if (isTransitioning) return;

        isTransitioning = true;

        // Show rest before transitioning to next workout (only if not the last one)
        if (currentWorkoutIndex < workouts.length - 1) {
            timeLeft = 60; // 1 minute rest time between workouts
            timerText.setText("Resting...");
            setInfoText.setText("Rest before next workout");

            // Update notification for resting
            Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_IS_RESTING, true);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, timeLeft);
            serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, "Rest Period");
            startService(serviceIntent);

            // Countdown for rest before moving to next workout
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (timeLeft > 0) {
                        updateTimerText();
                        timeLeft--;
                        updateServiceTimer();
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        currentWorkoutIndex++;
                        animateWorkoutTransition(() -> {
                            setupWorkout();
                            isTransitioning = false;
                        });
                    }
                }
            };

            timerHandler.postDelayed(timerRunnable, 1000);
            isRunning = true;

        } else {
            // All workouts completed
            sendWorkoutCompletedNotification(); // Send for last workout
            new AlertDialog.Builder(this)
                    .setTitle("Good Job!")
                    .setMessage("You have completed all workouts.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
            isTransitioning = false;
            stopService(new Intent(this, WorkoutForegroundService.class));
        }
    }


    private void updateSetInfo() {
        Workout current = workouts[currentWorkoutIndex];
        if (current.getRepsPerSet() == null) return;

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= current.getRepsPerSet().size(); i++) {
            if (completedSets.contains(i)) {
                builder.append("✅");
            } else {
                builder.append(current.getRepsPerSet().get(i - 1));
            }

            if (i < current.getRepsPerSet().size()) {
                builder.append("-");
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


    //    permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendWorkoutCancelledNotification(workouts[currentWorkoutIndex].getTitle());
            }
        }
    }

    private void sendWorkoutCancelledNotification(String workoutTitle) {
        String channelId = "workout_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Workout Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Workout Cancelled")
                .setContentText("Today's workout \"" + workoutTitle + "\" was cancelled.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// Check permission before posting the notification (for Android 13+)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build());
        } else {
            // Optionally, request permission or handle denial
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

    }


    private void sendWorkoutCompletedNotification() {
        String channelId = "workout_channel";

        // Get the current day (e.g., Tuesday, etc.)
        String currentDay = new java.text.SimpleDateFormat("EEEE", Locale.getDefault()).format(new java.util.Date());
        Log.d("WorkoutActivity", "Sending notification for " + currentDay + "'s workout.");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Workout Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(currentDay + "'s Workout Completed")
                .setContentText("Great job! You've completed today's workout.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check permission before posting the notification (for Android 13+)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build()); // ID for completed workout notification
            Log.d("WorkoutActivity", "Notification sent successfully.");
        } else {
            // Optionally, request permission or handle denial
            Log.d("WorkoutActivity", "Notification permission not granted.");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }



    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();

        // Unregister the receiver when the activity is destroyed
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver);
            isReceiverRegistered = false;
        }
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
