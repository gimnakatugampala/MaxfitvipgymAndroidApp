package com.example.maxfitvipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.R;
import com.google.android.material.button.MaterialButton;

public class PhoneNumberActivity extends AppCompatActivity {

    private EditText phoneNumberInput;
    private MaterialButton btnGetStarted;
    private TextView caption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);

        // Initialize views
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        caption = findViewById(R.id.caption);

        // Set up button click listener
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the phone number from the input field
                String phoneNumber = phoneNumberInput.getText().toString().trim();

                // Check if the phone number is valid (numeric and at least 10 digits)
                if (isValidPhoneNumber(phoneNumber)) {
                    // Proceed to the next screen, e.g., phone number verification screen
                    Intent intent = new Intent(PhoneNumberActivity.this, VerificationPhoneNumberActivity.class);
                    intent.putExtra("phoneNumber", phoneNumber);  // Passing phone number to the next screen
                    startActivity(intent);
                    finish(); // Finish the current activity to prevent going back to this screen
                } else {
                    // Show an error or prompt user to enter a valid phone number
                    phoneNumberInput.setError("Please enter a valid phone number");
                }
            }
        });
    }

    // Validate phone number (numeric and at least 10 digits)
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Check if the phone number is numeric and has at least 10 digits
        return phoneNumber != null && !phoneNumber.isEmpty() && phoneNumber.matches("\\d{10,}");  // Ensures it is numeric and at least 10 digits long
    }
}
