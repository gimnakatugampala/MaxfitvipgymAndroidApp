package com.maxfit.vipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.maxfit.vipgymapp.Model.Member;
import com.maxfit.vipgymapp.R;
import com.maxfit.vipgymapp.Repository.MemberRepository;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneNumberActivity extends AppCompatActivity {

    private EditText phoneNumberInput;
    private MaterialButton btnGetStarted;
    private TextView caption;
    private ProgressBar progressBar;
    private MemberRepository memberRepository;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);

        // Initialize views
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        caption = findViewById(R.id.caption);

        // Add progress bar
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        memberRepository = new MemberRepository();
        executorService = Executors.newSingleThreadExecutor();

        // Set up button click listener
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = phoneNumberInput.getText().toString().trim();

                if (isValidPhoneNumber(phoneNumber)) {
                    checkMemberExists(phoneNumber);
                } else {
                    phoneNumberInput.setError("Please enter a valid phone number");
                }
            }
        });
    }

    private void checkMemberExists(String phoneNumber) {
        btnGetStarted.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                Member member = memberRepository.getMemberByPhone(phoneNumber);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGetStarted.setEnabled(true);

                    // Always proceed to verification/registration screen
                    // The member will be created if they don't exist
                    Intent intent = new Intent(PhoneNumberActivity.this,
                            VerificationPhoneNumberActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);

                    if (member != null) {
                        intent.putExtra("memberId", member.getId());
                        intent.putExtra("memberExists", true);
                    } else {
                        intent.putExtra("memberExists", false);
                    }

                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGetStarted.setEnabled(true);
                    Toast.makeText(PhoneNumberActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && !phoneNumber.isEmpty() &&
                phoneNumber.matches("\\d{10,}");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}