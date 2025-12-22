package com.maxfit.vipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.MemberRepository;
import com.maxfit.vipgymapp.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaitingApprovalActivity extends AppCompatActivity {

    private static final String TAG = "WaitingApproval";
    private static final long CHECK_INTERVAL = 10000; // Check every 10 seconds

    private TextView titleText;
    private TextView messageText;
    private TextView statusText;
    private MaterialButton btnCheckStatus;
    private MaterialButton btnLogout;

    private String phoneNumber;
    private String firstName;
    private MemberRepository memberRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable checkStatusRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_approval);

        // Initialize views
        titleText = findViewById(R.id.titleText);
        messageText = findViewById(R.id.messageText);
        statusText = findViewById(R.id.statusText);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        btnLogout = findViewById(R.id.btnLogout);

        // Get data from intent
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        firstName = getIntent().getStringExtra("firstName");

        memberRepository = new MemberRepository();
        sessionManager = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler();

        // Set personalized message
        if (firstName != null && !firstName.isEmpty()) {
            titleText.setText("Thank you, " + firstName + "!");
        }

        // Set up buttons
        btnCheckStatus.setOnClickListener(v -> checkApprovalStatus());
        btnLogout.setOnClickListener(v -> logout());

        // Start automatic status checking
        startAutoStatusCheck();

        // Initial status check
        checkApprovalStatus();
    }

    private void startAutoStatusCheck() {
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                checkApprovalStatus();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.postDelayed(checkStatusRunnable, CHECK_INTERVAL);
    }

    private void stopAutoStatusCheck() {
        if (handler != null && checkStatusRunnable != null) {
            handler.removeCallbacks(checkStatusRunnable);
        }
    }

    private void checkApprovalStatus() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Error: No phone number found", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCheckStatus.setEnabled(false);
        statusText.setText("⏳ Checking status...");
        statusText.setTextColor(getResources().getColor(android.R.color.white));

        executorService.execute(() -> {
            try {
                Member member = memberRepository.getMemberByPhone(phoneNumber);

                runOnUiThread(() -> {
                    btnCheckStatus.setEnabled(true);

                    if (member == null) {
                        statusText.setText("❌ Error: Member not found");
                        statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        Log.e(TAG, "Member not found with phone: " + phoneNumber);
                        return;
                    }

                    Log.d(TAG, "Member found - is_active: " + member.isActive());

                    if (member.isActive()) {
                        // ✅ Member has been approved!
                        statusText.setText("✅ Approved! Redirecting...");
                        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));

                        Toast.makeText(this, "Your account has been approved!", Toast.LENGTH_LONG).show();

                        // Stop checking
                        stopAutoStatusCheck();

                        // Save session
                        sessionManager.createLoginSession(member);

                        // Redirect to home after 2 seconds
                        handler.postDelayed(() -> {
                            Intent intent = new Intent(WaitingApprovalActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }, 2000);

                    } else {
                        // Still waiting for approval
                        statusText.setText("⏳ Pending admin approval");
                        statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                        Log.d(TAG, "Member still not approved");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error checking approval status", e);
                runOnUiThread(() -> {
                    btnCheckStatus.setEnabled(true);
                    statusText.setText("❌ Error checking status");
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void logout() {
        stopAutoStatusCheck();

        // Don't create session if not approved
        sessionManager.logout();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, GetStartedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check status when returning to this activity
        checkApprovalStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop checking when activity is not visible
        stopAutoStatusCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoStatusCheck();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back - user must either wait or logout
        Toast.makeText(this, "Please wait for approval or logout", Toast.LENGTH_SHORT).show();
    }
}