package com.maxfit.vipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.maxfit.vipgymapp.R;
import com.google.android.material.button.MaterialButton;

public class VerificationPhoneNumberActivity extends AppCompatActivity {

    private EditText codeInput1, codeInput2, codeInput3, codeInput4, codeInput5;
    private MaterialButton btnVerify;

    private String phoneNumber;
    private String expectedOtp; // The code sent via SMS
    private int memberId;
    private boolean memberExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // Get data from intent
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        expectedOtp = getIntent().getStringExtra("otp"); // Retrieve the OTP generated in previous screen
        memberId = getIntent().getIntExtra("memberId", -1);
        memberExists = getIntent().getBooleanExtra("memberExists", false);

        codeInput1 = findViewById(R.id.codeInput1);
        codeInput2 = findViewById(R.id.codeInput2);
        codeInput3 = findViewById(R.id.codeInput3);
        codeInput4 = findViewById(R.id.codeInput4);
        codeInput5 = findViewById(R.id.codeInput5);
        btnVerify = findViewById(R.id.btnVerify);

        setupInputs();

        btnVerify.setOnClickListener(v -> {
            String enteredCode = codeInput1.getText().toString() +
                    codeInput2.getText().toString() +
                    codeInput3.getText().toString() +
                    codeInput4.getText().toString() +
                    codeInput5.getText().toString();

            if (enteredCode.length() == 5) {
                // Verify against the actual OTP sent
                if (enteredCode.equals(expectedOtp)) {
                    Toast.makeText(VerificationPhoneNumberActivity.this, "Verification Successful!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(VerificationPhoneNumberActivity.this, MembershipDetailsActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);
                    intent.putExtra("memberId", memberId);
                    intent.putExtra("memberExists", memberExists);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerificationPhoneNumberActivity.this, "Invalid Code. Please try again.", Toast.LENGTH_SHORT).show();
                    clearInputs();
                }
            } else {
                Toast.makeText(VerificationPhoneNumberActivity.this, "Please enter the full 5-digit code", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearInputs() {
        codeInput1.setText("");
        codeInput2.setText("");
        codeInput3.setText("");
        codeInput4.setText("");
        codeInput5.setText("");
        codeInput1.requestFocus();
    }

    private void setupInputs() {
        setKeyListener(codeInput1, null, codeInput2);
        setKeyListener(codeInput2, codeInput1, codeInput3);
        setKeyListener(codeInput3, codeInput2, codeInput4);
        setKeyListener(codeInput4, codeInput3, codeInput5);
        setKeyListener(codeInput5, codeInput4, null);
    }

    private void setKeyListener(final EditText currentField, final EditText previousField, final EditText nextField) {
        currentField.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (currentField.getText().length() == 0 && previousField != null) {
                    previousField.requestFocus();
                }
            }
            // Move forward logic is usually handled by TextWatcher, but keeping your key listener approach
            if (currentField.getText().length() == 1 && keyCode != KeyEvent.KEYCODE_DEL && nextField != null) {
                nextField.requestFocus();
            }
            return false;
        });

        // Adding simple text changed listener for smoother forward navigation
        currentField.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && nextField != null) {
                    nextField.requestFocus();
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }
}