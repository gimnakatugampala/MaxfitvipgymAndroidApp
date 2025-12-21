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
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.maxfitvipgymapp.Activity.WorkoutSettingsActivity;
import com.example.maxfitvipgymapp.Adapter.YouTubeAdapter;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Repository.WorkoutRepository;
import com.example.maxfitvipgymapp.Service.WorkoutForegroundService;
import com.example.maxfitvipgymapp.Utils.SessionManager;
import com.example.maxfitvipgymapp.Widget.WorkoutWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class WorkoutActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutActivity";

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

    // ✅ Text-to-Speech variables
    private TextToSpeech tts;
    private boolean isTTSReady = false;
    private AudioManager audioManager;
    private int originalMusicVolume;
    private AudioFocusRequest audioFocusRequest;
    private static final String TTS_UTTERANCE_ID = "workout_tts";

    // ✅ Voice announcement settings
    private boolean voiceGuidanceEnabled = true;
    private int countdownVoiceStart = 10;
    private List<Integer> countdownNumbers = Arrays.asList(10, 3, 2, 1);

    // ✅ Database integration
    private List<Workout> workouts = new ArrayList<>();
    private WorkoutRepository workoutRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private int currentMemberScheduleId = -1;

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

        // ✅ Initialize repositories
        workoutRepository = new WorkoutRepository();
        executorService = Executors.newSingleThreadExecutor();
        sessionManager = new SessionManager(this);

        // ✅ Initialize AudioManager first
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // ✅ Initialize Text-to-Speech
        initializeTextToSpeech();

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
            if (!workouts.isEmpty() && currentWorkoutIndex < workouts.size()) {
                stopTimer();
                playPauseButton.setImageResource(R.drawable.playbutton);
                List<String> videoUrls = workouts.get(currentWorkoutIndex).getYoutubeUrls();
                if (videoUrls != null && !videoUrls.isEmpty()) {
                    showYouTubeVideo(videoUrls.get(0));
                }
            }
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
                        if (!workouts.isEmpty() && currentWorkoutIndex < workouts.size()) {
                            sendWorkoutCancelledNotification(workouts.get(currentWorkoutIndex).getTitle());
                        }
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

        // ✅ Load workouts from database
        loadTodayWorkouts();
    }

    // ✅ NEW: Load today's workouts from database
    private void loadTodayWorkouts() {
        int memberId = sessionManager.getMemberId();

        if (memberId == -1) {
            Log.e(TAG, "Member ID not found");
            Toast.makeText(this, "Error: Member not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        executorService.execute(() -> {
            try {
                // Get member's active schedule
                Map<String, Object> memberSchedule = workoutRepository.getMemberWorkoutSchedule(memberId);

                if (memberSchedule == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No active workout schedule found", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                currentMemberScheduleId = (int) memberSchedule.get("id");

                // Get today's day name
                Calendar calendar = Calendar.getInstance();
                String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                String todayDayName = days[calendar.get(Calendar.DAY_OF_WEEK) - 1];

                Log.d(TAG, "Loading workouts for: " + todayDayName);

                // Get today's workout details
                List<Map<String, Object>> todayWorkoutDetails =
                        workoutRepository.getMemberWorkoutScheduleDetails(currentMemberScheduleId, todayDayName);

                if (todayWorkoutDetails == null || todayWorkoutDetails.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No workouts scheduled for today", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                // Check if it's a rest day
                boolean isRestDay = false;
                for (Map<String, Object> detail : todayWorkoutDetails) {
                    Boolean restDay = (Boolean) detail.get("is_rest_day");
                    if (restDay != null && restDay) {
                        isRestDay = true;
                        break;
                    }
                }

                if (isRestDay) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Today is a rest day!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                // Convert database workouts to Workout objects
                workouts.clear();
                for (Map<String, Object> detail : todayWorkoutDetails) {
                    String workoutName = (String) detail.get("name");
                    String description = (String) detail.get("description");
                    String imageUrl = (String) detail.get("image_url");

                    // Parse duration (in minutes from DB, convert to seconds)
                    String durationStr = (String) detail.get("duration_minutes");
                    int durationSeconds = 60; // default 60 seconds
                    if (durationStr != null && !durationStr.isEmpty()) {
                        try {
                            durationSeconds = Integer.parseInt(durationStr) * 60; // convert to seconds
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid duration: " + durationStr);
                        }
                    }

                    // Parse sets and reps
                    String setStr = (String) detail.get("set_no");
                    String repStr = (String) detail.get("rep_no");

                    List<Integer> repsPerSet = null;
                    boolean isDurationBased = true;

                    // If we have sets and reps, it's set-based
                    if (setStr != null && !setStr.isEmpty() && repStr != null && !repStr.isEmpty()) {
                        try {
                            int sets = Integer.parseInt(setStr);
                            int reps = Integer.parseInt(repStr);

                            isDurationBased = false;
                            repsPerSet = new ArrayList<>();
                            for (int i = 0; i < sets; i++) {
                                repsPerSet.add(reps);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid sets/reps: " + setStr + "/" + repStr);
                        }
                    }

                    // Get workout videos
                    int workoutId = (int) detail.get("workout_id");
                    List<String> videoUrls = workoutRepository.getWorkoutVideos(workoutId);

                    // Use default image if none provided
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        imageUrl = "https://images.pexels.com/photos/1552249/pexels-photo-1552249.jpeg";
                    }

                    Workout workout = new Workout(
                            workoutName != null ? workoutName : "Workout",
                            isDurationBased,
                            durationSeconds,
                            repsPerSet,
                            imageUrl,
                            videoUrls
                    );

                    workouts.add(workout);
                    Log.d(TAG, "Added workout: " + workoutName + " (duration: " + durationSeconds + "s, sets: " + (repsPerSet != null ? repsPerSet.size() : 0) + ")");
                }

                runOnUiThread(() -> {
                    if (workouts.isEmpty()) {
                        Toast.makeText(this, "No workouts found for today", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.d(TAG, "Loaded " + workouts.size() + " workouts");
                        setupWorkout(); // Start first workout
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading workouts", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading workouts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    // ✅ FIXED: Initialize Text-to-Speech with proper error handling
    private void initializeTextToSpeech() {
        Log.d(TAG, "Initializing TTS...");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS initialization successful");

                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported");
                    isTTSReady = false;
                    runOnUiThread(() -> Toast.makeText(this, "Voice guidance not available - language not supported", Toast.LENGTH_SHORT).show());
                    return;
                }

                // ✅ Load settings
                voiceGuidanceEnabled = WorkoutSettingsActivity.isVoiceGuidanceEnabled(this);
                float speechRate = WorkoutSettingsActivity.getSpeechRate(this);
                float speechPitch = WorkoutSettingsActivity.getSpeechPitch(this);
                countdownVoiceStart = WorkoutSettingsActivity.getCountdownStart(this);

                // ✅ Configure TTS
                tts.setSpeechRate(speechRate);
                tts.setPitch(speechPitch);

                // ✅ CRITICAL: Use STREAM_MUSIC for audio output
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build();
                    tts.setAudioAttributes(audioAttributes);
                }

                // ✅ Set up utterance listener
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "🎤 TTS started speaking: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "✅ TTS finished speaking: " + utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "❌ TTS error: " + utteranceId);
                    }
                });

                isTTSReady = true;
                Log.d(TAG, "✅ TTS ready - Voice guidance: " + voiceGuidanceEnabled);

                // ✅ Increase media volume if too low
                if (audioManager != null) {
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    Log.d(TAG, "📢 Current media volume: " + currentVolume + "/" + maxVolume);

                    if (currentVolume < maxVolume / 3) {
                        Log.d(TAG, "⚠️ Volume too low, increasing...");
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0);
                    }
                }

                // ✅ Test TTS immediately
                runOnUiThread(() -> testTTS());

            } else {
                Log.e(TAG, "TTS initialization failed with status: " + status);
                isTTSReady = false;
                runOnUiThread(() -> Toast.makeText(this, "Voice guidance not available", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ✅ NEW: Test TTS to verify it's working
    private void testTTS() {
        if (!isTTSReady) {
            Log.e(TAG, "❌ Cannot test TTS - not ready");
            return;
        }

        // ✅ FORCE MAXIMUM MEDIA VOLUME
        if (audioManager != null) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            Log.d(TAG, "📢 Current media volume BEFORE: " + currentVolume + "/" + maxVolume);

            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
            );

            int newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            Log.d(TAG, "🔊 FORCED MEDIA VOLUME TO: " + newVolume + "/" + maxVolume);

            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true);
            Log.d(TAG, "📢 FORCED SPEAKERPHONE ON");
        }

        new Handler().postDelayed(() -> {
            if (voiceGuidanceEnabled) {
                Log.d(TAG, "🎤 === TESTING TTS: 'Voice guidance ready' ===");
                speak("Voice guidance ready");
            } else {
                Log.d(TAG, "⚠️ Voice guidance DISABLED in settings");
                Toast.makeText(this, "Voice guidance is disabled. Enable it in settings.", Toast.LENGTH_LONG).show();
            }
        }, 2000);
    }

    // ✅ FIXED: Abandon audio focus properly
    private void abandonAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            Log.d(TAG, "Audio focus abandoned (API 26+)");
        } else {
            audioManager.abandonAudioFocus(null);
            Log.d(TAG, "Audio focus abandoned (Legacy)");
        }
    }

    // ✅ FIXED: Speak with proper parameters and logging
    private void speak(String text) {
        if (!isTTSReady) {
            Log.w(TAG, "TTS not ready, cannot speak: " + text);
            return;
        }

        if (!voiceGuidanceEnabled) {
            Log.d(TAG, "Voice guidance disabled, skipping: " + text);
            return;
        }

        if (tts == null) {
            Log.e(TAG, "TTS is null, cannot speak: " + text);
            return;
        }

        Log.d(TAG, "=== SPEAKING: " + text + " ===");

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_UTTERANCE_ID);
        params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));

        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "❌ TTS speak() returned ERROR");
        } else {
            Log.d(TAG, "✅ TTS speak() called successfully");
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "📢 Media volume: " + currentVolume + "/" + maxVolume);
            }
        }
    }

    // ✅ Announce workout start
    private void announceWorkoutStart(Workout workout) {
        String announcement;
        if (workout.isDurationBased()) {
            announcement = String.format("Starting %s. Duration: %d seconds. Go!",
                    workout.getTitle(), workout.getTime());
        } else {
            announcement = String.format("Starting %s. Set %d of %d. %d repetitions. Go!",
                    workout.getTitle(), currentSet, workout.getRepsPerSet().size(),
                    workout.getRepsPerSet().get(currentSet - 1));
        }
        speak(announcement);
    }

    // ✅ Announce rest period
    private void announceRestPeriod(int seconds, String nextActivity) {
        String announcement = String.format("Rest for %d seconds. Next: %s", seconds, nextActivity);
        speak(announcement);
    }

    // ✅ Announce countdown
    private void announceCountdown(int seconds) {
        if (countdownNumbers.contains(seconds)) {
            speak(String.valueOf(seconds));
        }
    }

    // ✅ Announce set completion
    private void announceSetCompletion(int completedSet, int totalSets) {
        String announcement;
        if (completedSet < totalSets) {
            announcement = String.format("Set %d complete. Rest before set %d", completedSet, completedSet + 1);
        } else {
            announcement = "Great job! All sets completed.";
        }
        speak(announcement);
    }

    // ✅ Announce workout completion
    private void announceWorkoutCompletion() {
        speak("Workout complete! Excellent work!");
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
        if (workouts.isEmpty() || currentWorkoutIndex >= workouts.size()) {
            Log.e(TAG, "No workout available at index: " + currentWorkoutIndex);
            return;
        }

        Workout workout = workouts.get(currentWorkoutIndex);
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

        // ✅ Announce workout start after 2 second delay
        new Handler().postDelayed(() -> announceWorkoutStart(workout), 2000);

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
                    updateTimerText();
                    updateServiceTimer();

                    // ✅ Announce countdown numbers
                    if (timeLeft <= countdownVoiceStart) {
                        announceCountdown(timeLeft);
                    }

                    timeLeft--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    handleTimerCompletion();
                }
            }
        };

        updateTimerText();
        updateServiceTimer();
        timerHandler.postDelayed(timerRunnable, 1000);
        isRunning = true;
    }

    private void updateServiceTimer() {
        if (workouts.isEmpty() || currentWorkoutIndex >= workouts.size()) {
            return;
        }

        Intent serviceIntent = new Intent(this, WorkoutForegroundService.class);
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_DURATION, timeLeft);
        serviceIntent.putExtra(WorkoutForegroundService.EXTRA_WORKOUT_TITLE, workouts.get(currentWorkoutIndex).getTitle());
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
        if (workouts.isEmpty() || currentWorkoutIndex >= workouts.size()) {
            return;
        }

        Workout current = workouts.get(currentWorkoutIndex);
        playSoundEffect();

        if (!current.isDurationBased() && current.getRepsPerSet() != null) {
            if (isResting) {
                isResting = false;
                currentSet++;
                timeLeft = current.getTime();
                workoutTitle.setText(current.getTitle());
                updateSetInfo();
                updateServiceTimer();

                // ✅ Announce new set
                announceWorkoutStart(current);

                startTimer();
            } else {
                completedSets.add(currentSet);

                // ✅ Announce set completion
                announceSetCompletion(currentSet, current.getRepsPerSet().size());

                if (currentSet < current.getRepsPerSet().size()) {
                    isResting = true;
                    timeLeft = 60;
                    workoutTitle.setText("REST - " + current.getTitle());
                    setInfoText.setText("Rest before Set " + (currentSet + 1));

                    // ✅ Announce rest
                    announceRestPeriod(60, "Set " + (currentSet + 1));

                    updateServiceTimer();
                    startTimer();
                } else {
                    sendWorkoutCompletedNotification();
                    announceWorkoutCompletion();
                    moveToNextWorkout();
                }
            }
        } else {
            sendWorkoutCompletedNotification();
            announceWorkoutCompletion();
            moveToNextWorkout();
        }
    }

    private void moveToNextWorkout() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (currentWorkoutIndex < workouts.size() - 1) {
            isResting = true;
            timeLeft = 60;
            timerText.setText("1:00");
            workoutTitle.setText("REST");
            setInfoText.setText("Rest before next workout");
            setInfoText.setVisibility(View.VISIBLE);

            // ✅ Announce rest before next workout
            announceRestPeriod(60, workouts.get(currentWorkoutIndex + 1).getTitle());

            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (timeLeft > 0) {
                        updateTimerText();
                        updateServiceTimer();

                        // ✅ Countdown announcements
                        if (timeLeft <= countdownVoiceStart) {
                            announceCountdown(timeLeft);
                        }

                        timeLeft--;
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

            updateTimerText();
            updateServiceTimer();
            timerHandler.postDelayed(timerRunnable, 1000);
        } else {
            updateStreak();
            updateWidget();
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
        if (workouts.isEmpty() || currentWorkoutIndex >= workouts.size()) {
            return;
        }

        Workout current = workouts.get(currentWorkoutIndex);
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
        if (workouts.isEmpty() || currentWorkoutIndex >= workouts.size()) {
            return;
        }

        youtubeModal.setVisibility(View.VISIBLE);
        List<String> videoList = workouts.get(currentWorkoutIndex).getYoutubeUrls();

        if (videoList == null || videoList.isEmpty()) {
            Toast.makeText(this, "No videos available for this workout", Toast.LENGTH_SHORT).show();
            youtubeModal.setVisibility(View.GONE);
            return;
        }

        YouTubeAdapter adapter = new YouTubeAdapter(videoList);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!workouts.isEmpty() && currentWorkoutIndex < workouts.size()) {
                    sendWorkoutCancelledNotification(workouts.get(currentWorkoutIndex).getTitle());
                }
            }
        }
    }

    private void sendWorkoutCancelledNotification(String workoutTitle) {
        if (workoutTitle == null) {
            workoutTitle = "Workout";
        }

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
        // ✅ Clean up Text-to-Speech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS shut down");
        }

        // ✅ Abandon audio focus
        abandonAudioFocus();

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // ✅ Shutdown executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        super.onDestroy();

        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ✅ Stop TTS when app goes to background
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    // ✅ Inner Workout class
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