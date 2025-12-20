package com.example.maxfitvipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.maxfitvipgymapp.Adapter.SliderAdapter;
import com.example.maxfitvipgymapp.Model.SlideItem;
import com.example.maxfitvipgymapp.R;

import java.util.ArrayList;
import java.util.List;

public class GetStartedActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnGetStarted;
    private List<SlideItem> slides;

    // Declare handler at the class level
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        btnGetStarted = findViewById(R.id.btnGetStarted);

        // Create slides data
        slides = new ArrayList<>();
        slides.add(new SlideItem(R.drawable.slide1, "Protect Your Health Companion", "Elevate Fitness Journey with a Cutting-Edge to Fuel Your Motivation & Crush Your Goals"));
        slides.add(new SlideItem(R.drawable.slide2, "Build Strength & Confidence", "Stay consistent, stay dedicated, and transform yourself every day."));
        slides.add(new SlideItem(R.drawable.slide3, "Your Fitness, Your Rules", "Workout at your own pace and achieve your dream physique!"));

        // Set the adapter for ViewPager2
        viewPager.setAdapter(new SliderAdapter(slides));

        // Auto-slide functionality
        final int delay = 3000; // 3 seconds interval
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = viewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % slides.size();  // Loop back to the first item
                viewPager.setCurrentItem(nextItem, true); // True for smooth scrolling
                handler.postDelayed(this, delay); // Repeat every 3 seconds
            }
        };
        handler.postDelayed(runnable, delay); // Start auto-sliding immediately

        // Button click listener to navigate to next activity (PhoneNumberActivity)
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(GetStartedActivity.this, PhoneNumberActivity.class);
            startActivity(intent);
            finish(); // Close GetStartedActivity so it's not in the back stack
        });

        // Add this in onCreate after setting up btnGetStarted
        Button btnTest = new Button(this);
        btnTest.setText("Test Database Connection");
        btnTest.setOnClickListener(v -> {
            Intent intent = new Intent(GetStartedActivity.this, TestConnectionActivity.class);
            startActivity(intent);
        });
    }

    // Optional: stop the auto-sliding when the activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null); // Stop auto-sliding
    }

    // Optional: restart the auto-sliding when the activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(runnable, 3000); // Restart auto-sliding
    }
}
