package com.example.maxfitvipgymapp.Activity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.example.maxfitvipgymapp.Widget.WorkoutWidgetProvider;

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
    private ScrollView scrollContainer;

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

    private Workout[] workouts = {
            new Workout("WEIGHT LIFTING", false, 10, Arrays.asList(10, 10, 10),
                    "https://images.pexels.com/photos/3289711/pexels-photo-3289711.jpeg",
                    Arrays.asList("dQw4w9WgXcQ", "kXYiU_JCYtU")),
            new Workout("CARDIO BLAST", true, 10, null,
                    "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg",
                    Arrays.asList("9bZkp7q19f0")),
            new Workout("PUSH UPS", false, 10, Arrays.asList(15, 15, 15),
                    "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg",
                    Arrays.asList("9bZkp7q19f0", "dQw4w9WgXcQ"))
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

        workoutTitle = findViewById(R.id.workoutTitle);
        timerText = findViewById(R.id.timerText);
        playPauseButton = findViewById(R.id.playPauseButton);
        backgroundImage = findViewById(R.id.backgroundImage);
        setInfoText = findViewById(R.id.setInfoText);
        showVideoButton = findViewById(R.id.showVideoButton);
        backFromWorkout = findViewById(R.id.backFromWorkout);
        youtubeModal = findViewById(R.id.youtubeModal);
        closeYoutubeButton = findViewById(R.id.closeYoutubeButton);
        scrollContainer = findViewById(R.id.scrollContainer);

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
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            onSwipeUp();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        scrollContainer.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
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

    private void onSwipeUp() {
        if (isTransitioning && !isResting) return;

        Log.d("WorkoutActivity", "Swipe up detected - Skipping current timer/rest");

        stopTimer();
        timeLeft = 0;

        if (timerRunnable != null) {
            timerRunnable.run();
        }
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

        if (!workout.isDurationBased() && workout.getRepsPerSet() != null) {
            setInfoText.setVisibility(View.VISIBLE);
            updateSetInfo();
        } else {
            setInfoText.setVisibility(View.GONE);
        }

        scrollContainer.post(() -> scrollContainer.smoothScrollTo(0, 0));

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

        if (!current.isDurationBased() && current.getRepsPerSet() != null) {
            if (isResting) {
                isResting = false;
                currentSet++;
                timeLeft = current.getTime();
                workoutTitle.setText(current.getTitle());
                updateSetInfo();
                updateServiceTimer();
                startTimer();
            } else {
                completedSets.add(currentSet);

                if (currentSet < current.getRepsPerSet().size()) {
                    isResting = true;
                    timeLeft = 60;
                    workoutTitle.setText("REST - " + current.getTitle());
                    setInfoText.setText("Rest before Set " + (currentSet + 1));
                    updateServiceTimer();
                    startTimer();
                } else {
                    sendWorkoutCompletedNotification();
                    moveToNextWorkout();
                }
            }
        } else {
            sendWorkoutCompletedNotification();
            moveToNextWorkout();
        }
    }

    private void moveToNextWorkout() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (currentWorkoutIndex < workouts.length - 1) {
            isResting = true;
            timeLeft = 60;
            timerText.setText("1:00");
            workoutTitle.setText("REST");
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
                        isResting = false;
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
            // ✅ ALL WORKOUTS COMPLETED - UPDATE STREAK AND WIDGET
            updateStreak();
            updateWidget(); // ✅ ADD THIS LINE
            sendWorkoutCompletedNotification();
            showCelebrationAnimation();
        }
    }

    private void showCelebrationAnimation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View celebrationView = getLayoutInflater().inflate(R.layout.dialog_celebration, null);
        builder.setView(celebrationView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView celebrationIcon = celebrationView.findViewById(R.id.celebrationIcon);
        TextView celebrationTitle = celebrationView.findViewById(R.id.celebrationTitle);
        TextView celebrationMessage = celebrationView.findViewById(R.id.celebrationMessage);
        TextView streakInfo = celebrationView.findViewById(R.id.streakInfo);
        Button btnContinue = celebrationView.findViewById(R.id.btnContinueToDashboard);

        SharedPreferences prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE);
        int currentStreak = prefs.getInt("currentStreak", 0);

        streakInfo.setText("🔥 " + currentStreak + " Day Streak!");

        String[] messages = {
                "You're crushing it!",
                "Outstanding effort!",
                "You're on fire!",
                "Keep up the amazing work!",
                "You're unstoppable!",
                "Incredible dedication!",
                "You're a champion!"
        };
        int randomIndex = (int) (Math.random() * messages.length);
        celebrationMessage.setText(messages[randomIndex]);

        celebrationIcon.setScaleX(0f);
        celebrationIcon.setScaleY(0f);
        celebrationIcon.setAlpha(0f);
        celebrationIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        celebrationIcon.animate()
                .rotation(360f)
                .setDuration(800)
                .start();

        celebrationTitle.setAlpha(0f);
        celebrationTitle.setTranslationY(-50f);
        celebrationTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();

        celebrationMessage.setAlpha(0f);
        celebrationMessage.setTranslationY(-30f);
        celebrationMessage.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(400)
                .start();

        streakInfo.setAlpha(0f);
        streakInfo.setScaleX(0.8f);
        streakInfo.setScaleY(0.8f);
        streakInfo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(600)
                .start();

        btnContinue.setAlpha(0f);
        btnContinue.setTranslationY(50f);
        btnContinue.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(800)
                .start();

        try {
            MediaPlayer celebrationSound = MediaPlayer.create(this, R.raw.ding);
            celebrationSound.start();
            celebrationSound.setOnCompletionListener(mp -> mp.release());
        } catch (Exception e) {
            Log.e("WorkoutActivity", "Error playing celebration sound", e);
        }

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
            intent.putExtra("navigateTo", "home");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
        isTransitioning = false;
    }

    private void updateStreak() {
        SharedPreferences prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String lastWorkoutDate = prefs.getString("lastWorkoutDate", "");
        int currentStreak = prefs.getInt("currentStreak", 0);

        if (!today.equals(lastWorkoutDate)) {
            Calendar lastDate = Calendar.getInstance();
            Calendar todayDate = Calendar.getInstance();

            if (!lastWorkoutDate.isEmpty()) {
                try {
                    lastDate.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastWorkoutDate));
                    long diffInMillis = todayDate.getTimeInMillis() - lastDate.getTimeInMillis();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                    if (diffInDays == 1) {
                        currentStreak++;
                    } else if (diffInDays > 1) {
                        currentStreak = 1;
                    }
                } catch (Exception e) {
                    currentStreak = 1;
                }
            } else {
                currentStreak = 1;
            }

            editor.putString("lastWorkoutDate", today);
            editor.putInt("currentStreak", currentStreak);
            editor.apply();

            Log.d("WorkoutActivity", "Streak updated to: " + currentStreak);
        }
    }

    // ✅ ADD THIS NEW METHOD
    private void updateWidget() {
        Intent intent = new Intent(this, WorkoutWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, WorkoutWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);

        Log.d("WorkoutActivity", "Widget update broadcast sent");
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
                scrollContainer.post(() -> scrollContainer.smoothScrollTo(0, 0));
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
        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new java.util.Date());

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