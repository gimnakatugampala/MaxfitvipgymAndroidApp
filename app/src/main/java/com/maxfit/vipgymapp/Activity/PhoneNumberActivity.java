package com.maxfit.vipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneNumberActivity extends AppCompatActivity {

    private EditText phoneNumberInput;
    private MaterialButton btnGetStarted;
    private TextView caption;
    private ProgressBar progressBar;
    private MemberRepository memberRepository;
    private ExecutorService executorService;

    // --- CONFIGURATION ---
    private static final String API_USER_ID = "26652";
    private static final String API_KEY = "g0ueyuIip9LW8vzOBs8O";
    private static final String SENDER_ID = "MAXFIT GYM";

    // GOOGLE PLAY REVIEW CREDENTIALS
    private static final String GOOGLE_TEST_PHONE = "0777123456"; // Give this number to Google
    private static final String GOOGLE_TEST_OTP = "12345";      // Give this code to Google

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);

        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        caption = findViewById(R.id.caption);

        progressBar = new ProgressBar(this);
        // If you added the ProgressBar to XML, use findViewById instead.
        // If strictly creating programmatically, ensure it's added to the view hierarchy.
        // Assuming for now it works as per your previous code or is overlayed in XML.
        progressBar.setVisibility(View.GONE);

        memberRepository = new MemberRepository();
        executorService = Executors.newSingleThreadExecutor();

        btnGetStarted.setOnClickListener(v -> {
            String phoneNumber = phoneNumberInput.getText().toString().trim();

            if (isValidPhoneNumber(phoneNumber)) {
                // Start the process: Generate OTP -> Send SMS -> Check Member -> Navigate
                processPhoneNumber(phoneNumber);
            } else {
                phoneNumberInput.setError("Please enter a valid phone number");
            }
        });
    }

    private void processPhoneNumber(String phoneNumber) {
        btnGetStarted.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                // 1. Generate OTP
                String otp;
                if (phoneNumber.equals(GOOGLE_TEST_PHONE)) {
                    otp = GOOGLE_TEST_OTP; // Fixed code for Google Team
                } else {
                    otp = generateOTP();
                    // 2. Send SMS (Only if not the test number)
                    sendSms(phoneNumber, otp);
                }

                // 3. Check if member exists
                Member member = memberRepository.getMemberByPhone(phoneNumber);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGetStarted.setEnabled(true);

                    // 4. Navigate to Verification Screen with the OTP
                    Intent intent = new Intent(PhoneNumberActivity.this, VerificationPhoneNumberActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);
                    intent.putExtra("otp", otp); // Pass the generated OTP

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
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGetStarted.setEnabled(true);
                    Toast.makeText(PhoneNumberActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void sendSms(String phoneNumber, String otp) throws IOException {
        // Format number: Remove leading 0 and add 94 (e.g., 0771234567 -> 94771234567)
        String formattedNumber = phoneNumber;
        if (formattedNumber.startsWith("0")) {
            formattedNumber = "94" + formattedNumber.substring(1);
        }

        String message = "Your MaxFit Verification Code is: " + otp;

        // Notify.lk API URL
        String urlString = "https://app.notify.lk/api/v1/send?user_id=" + API_USER_ID +
                "&api_key=" + API_KEY +
                "&sender_id=" + SENDER_ID.replace(" ", "%20") +
                "&to=" + formattedNumber +
                "&message=" + message.replace(" ", "%20");

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to send SMS. Response: " + responseCode);
        }
        conn.disconnect();
    }

    private String generateOTP() {
        Random random = new Random();
        int code = 10000 + random.nextInt(90000); // Generates 10000 to 99999
        return String.valueOf(code);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && !phoneNumber.isEmpty() && phoneNumber.matches("\\d{10,}");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}