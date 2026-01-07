package com.maxfit.vipgymapp.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500; // 2.5 seconds
    private ImageView logoImage;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoImage = findViewById(R.id.logoImage);
        sessionManager = new SessionManager(this);

        // --- VERSION CHECK LOGIC ---
        // If Android 12 (API 31) or higher -> Load original High-Res image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            logoImage.setImageResource(R.drawable.maxfit_logo_sm);
        }
        // If Android 11 or lower -> Use Glide to resize and prevent crashing
        else {
            Glide.with(this)
                    .load(R.drawable.maxfit_logo_sm)
                    .into(logoImage);
        }
        // ---------------------------

        // Load and start animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        logoImage.startAnimation(fadeIn);
        logoImage.startAnimation(scaleUp);

        // Navigate after delay
        new Handler().postDelayed(() -> {
            navigateToNextScreen();
        }, SPLASH_DURATION);
    }

    private void navigateToNextScreen() {
        Intent intent;

        // Check if user is logged in
        if (sessionManager.isLoggedIn()) {
            // Navigate to MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // Navigate to GetStartedActivity
            intent = new Intent(SplashActivity.this, GetStartedActivity.class);
        }

        startActivity(intent);
        finish();

        // Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}