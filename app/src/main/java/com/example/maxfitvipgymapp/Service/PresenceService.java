package com.example.maxfitvipgymapp.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.maxfitvipgymapp.Repository.PresenceRepository;
import com.example.maxfitvipgymapp.Utils.SessionManager;

/**
 * Background service to maintain member presence status
 * Sends heartbeat every 15 seconds to keep member marked as "online"
 */
public class PresenceService extends Service {

    private static final String TAG = "PresenceService";
    private static final long HEARTBEAT_INTERVAL = 15000; // 15 seconds

    public static final String ACTION_PRESENCE_UPDATE = "com.example.maxfitvipgymapp.PRESENCE_UPDATE";
    public static final String EXTRA_ONLINE_COUNT = "online_count";

    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private PresenceRepository presenceRepository;
    private SessionManager sessionManager;

    private int memberId = -1;
    private String sessionToken = null;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ PresenceService created");

        presenceRepository = new PresenceRepository();
        sessionManager = new SessionManager(this);
        heartbeatHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "📥 PresenceService started");

        if (!sessionManager.isLoggedIn()) {
            Log.w(TAG, "⚠️ No user logged in, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        memberId = sessionManager.getMemberId();

        if (memberId == -1) {
            Log.e(TAG, "❌ Invalid member ID, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Get device info
        String deviceInfo = getDeviceInfo();

        // Mark member as online
        new Thread(() -> {
            sessionToken = presenceRepository.markMemberOnline(memberId, deviceInfo);

            if (sessionToken != null) {
                Log.d(TAG, "✅ Member " + memberId + " marked online with token: " + sessionToken);
                startHeartbeat();
            } else {
                Log.e(TAG, "❌ Failed to mark member online");
            }
        }).start();

        return START_STICKY;
    }

    /**
     * Start sending periodic heartbeats
     */
    private void startHeartbeat() {
        if (isRunning) {
            Log.d(TAG, "⚠️ Heartbeat already running");
            return;
        }

        isRunning = true;
        Log.d(TAG, "💓 Starting heartbeat every " + (HEARTBEAT_INTERVAL / 1000) + " seconds");

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                // Send heartbeat in background thread
                new Thread(() -> {
                    boolean success = presenceRepository.sendHeartbeat(memberId, sessionToken);

                    if (success) {
                        Log.d(TAG, "💓 Heartbeat sent for member " + memberId);

                        // Get online member count and broadcast
                        int onlineCount = presenceRepository.getOnlineMemberCount();
                        broadcastPresenceUpdate(onlineCount);
                    } else {
                        Log.e(TAG, "❌ Failed to send heartbeat");
                    }
                }).start();

                // Schedule next heartbeat
                if (isRunning) {
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };

        // Start heartbeat loop
        heartbeatHandler.post(heartbeatRunnable);
    }

    /**
     * Stop sending heartbeats
     */
    private void stopHeartbeat() {
        isRunning = false;
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        Log.d(TAG, "⏹️ Heartbeat stopped");
    }

    /**
     * Broadcast presence update to app
     */
    private void broadcastPresenceUpdate(int onlineCount) {
        Intent intent = new Intent(ACTION_PRESENCE_UPDATE);
        intent.putExtra(EXTRA_ONLINE_COUNT, onlineCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Get device info string
     */
    private String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 PresenceService destroyed");

        stopHeartbeat();

        // Mark member as offline
        if (memberId != -1 && sessionToken != null) {
            new Thread(() -> {
                presenceRepository.markMemberOffline(memberId, sessionToken);
                Log.d(TAG, "👋 Member " + memberId + " marked offline");
            }).start();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}