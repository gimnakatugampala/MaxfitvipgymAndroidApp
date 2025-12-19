package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private boolean isResting = false;

    // Workout data - Mix of duration and strength-based
    private Workout[] workouts = {
            new Workout("WEIGHT LIFTING", false, 60, Arrays.asList(10, 10, 10),
                    "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg",
                    Arrays.asList("dQw4w9WgXcQ", "kXYiU_JCYtU")), // 3 sets x 10 reps
            new Workout("CARDIO BLAST", true, 300, null,
                    "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg",
                    Arrays.asList("9bZkp7q19f0")), // 5 minutes duration
            new Workout("PUSH UPS", false, 45, Arrays.asList(15, 15, 15),
                    "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg",
                    Arrays.asList("9bZkp7q19f0", "dQw4w9WgXcQ")) // 3 sets x 15 reps
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
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, new IntentFilter("TIMER_UPDATE"));
            isReceiverRegistered = true;
        }

        // Initialize views
        workoutTitle = findViewById(R.id.workoutTitle);
        timerText = findViewById(R.id.timerText);
        playPauseButton = findViewById(R.id.playPauseButton);
        backgroundImage = findViewById(R.id.backgroundImage);
        setInfoText = findViewById(R.id.setInfoText);
        showVideoButton = findViewById(R.id.showVideoButton);
        backFromWorkout = findViewById(R.id.backFromWorkout);
        youtubeModal = findViewById(R.id.youtubeModal);
        closeYoutubeButton = findViewById(R.id.closeYoutubeButton);

        showVideoButton.setOnClickListener(v -> {
            stopTimer();
            playPauseButton.setImageResource(R.drawable.playbutton);
            showYouTubeVideo(workouts[currentWorkoutIndex].getYoutubeUrls().get(0));
        });

        closeYoutubeButton.setOnClickListener(v -> {
            youtubeModal.setVisibility(View.GONE);
            playPauseButton.setImageResource(R.drawable.playbutton);
            if (!isRunning) {
                startTimer();
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
        isResting = false;

        // Start foreground service
        Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, workout.getTitle());
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, workout.getTime());
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_IS_RESTING, false);
        startService(serviceIntent);

        Glide.with(this)
                .load(workout.getImageUrl())
                .transition(withCrossFade())
                .into(backgroundImage);

        updateTimerText();

        // Show set info only for strength-based workouts
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

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    updateTimerText();
                    updateServiceTimer();
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
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, workouts[currentWorkoutIndex].getTitle());
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_IS_RESTING, isResting);
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

        // For strength-based workouts
        if (!current.isDurationBased() && current.getRepsPerSet() != null) {
            if (isResting) {
                // Rest period done, start next set
                isResting = false;
                currentSet++;
                timeLeft = current.getTime();
                workoutTitle.setText(current.getTitle());
                updateSetInfo();
                startTimer();
            } else {
                // Set completed
                completedSets.add(currentSet);

                if (currentSet < current.getRepsPerSet().size()) {
                    // Start rest period before next set
                    isResting = true;
                    timeLeft = 60; // 1 minute rest
                    workoutTitle.setText("REST - " + current.getTitle());
                    setInfoText.setText("Rest before Set " + (currentSet + 1));
                    startTimer();
                } else {
                    // All sets completed, move to next workout
                    sendWorkoutCompletedNotification();
                    moveToNextWorkout();
                }
            }
        } else {
            // Duration-based workout completed
            sendWorkoutCompletedNotification();
            moveToNextWorkout();
        }
    }

    private void moveToNextWorkout() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (currentWorkoutIndex < workouts.length - 1) {
            // Rest before next workout
            isResting = true;
            timeLeft = 60; // 1 minute rest
            timerText.setText("Resting...");
            setInfoText.setText("Rest before next workout");
            setInfoText.setVisibility(View.VISIBLE);

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
                        stopService(new Intent(WorkoutActivity.this, WorkoutForegroundService.class));
                    }
                }
            };

            timerHandler.postDelayed(timerRunnable, 1000);
        } else {
            // All workouts completed - Update streak
            updateStreak();
            sendWorkoutCompletedNotification();

            new AlertDialog.Builder(WorkoutActivity.this)
                    .setTitle("Great Job! 🎉")
                    .setMessage("You've completed all workouts for today! Your streak has been updated.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                        intent.putExtra("navigateTo", "home");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            isTransitioning = false;
        }
    }

    private void updateStreak() {
        SharedPreferences prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Get current date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String lastWorkoutDate = prefs.getString("lastWorkoutDate", "");
        int currentStreak = prefs.getInt("currentStreak", 0);

        // Check if workout already completed today
        if (!today.equals(lastWorkoutDate)) {
            // Calculate if streak should continue
            Calendar lastDate = Calendar.getInstance();
            Calendar todayDate = Calendar.getInstance();

            if (!lastWorkoutDate.isEmpty()) {
                try {
                    lastDate.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastWorkoutDate));
                    long diffInMillis = todayDate.getTimeInMillis() - lastDate.getTimeInMillis();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                    if (diffInDays == 1) {
                        // Consecutive day - increase streak
                        currentStreak++;
                    } else if (diffInDays > 1) {
                        // Streak broken - reset to 1
                        currentStreak = 1;
                    }
                } catch (Exception e) {
                    currentStreak = 1;
                }
            } else {
                // First workout
                currentStreak = 1;
            }

            editor.putString("lastWorkoutDate", today);
            editor.putInt("currentStreak", currentStreak);
            editor.apply();

            Log.d("WorkoutActivity", "Streak updated to: " + currentStreak);
        }
    }

    private void updateSetInfo() {
        Workout current = workouts[currentWorkoutIndex];
        if (current.getRepsPerSet() == null) return;

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= current.getRepsPerSet().size(); i++) {
            if (completedSets.contains(i)) {
                builder.append("✅");
            } else if (i == currentSet) {
                builder.append("[").append(current.getRepsPerSet().get(i - 1)).append("]");
            } else {
                builder.append(current.getRepsPerSet().get(i - 1));
            }

            if (i < current.getRepsPerSet().size()) {
                builder.append(" - ");
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
        List<String> videoList = workouts[currentWorkoutIndex].getYoutubeUrls();
        YouTubeAdapter adapter = new YouTubeAdapter(videoList);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
    }

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

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build());
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void sendWorkoutCompletedNotification() {
        String channelId = "workout_channel";
        String currentDay = new java.text.SimpleDateFormat("EEEE", Locale.getDefault()).format(new java.util.Date());

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

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();

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
        private List<String> youtubeUrls;

        public Workout(String title, boolean isDurationBased, int time, List<Integer> repsPerSet, String imageUrl, List<String> youtubeUrls) {
            this.title = title;
            this.isDurationBased = isDurationBased;
            this.time = time;
            this.repsPerSet = repsPerSet;
            this.imageUrl = imageUrl;
            this.youtubeUrls = youtubeUrls;
        }

        public String getTitle() { return title; }
        public boolean isDurationBased() { return isDurationBased; }
        public int getTime() { return time; }
        public List<Integer> getRepsPerSet() { return repsPerSet; }
        public String getImageUrl() { return imageUrl; }
        public List<String> getYoutubeUrls() { return youtubeUrls; }
    }
}