package com.example.maxfitvipgymapp.Activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.R;
import com.google.android.material.button.MaterialButton;

public class VerificationPhoneNumberActivity extends AppCompatActivity {

    // Declare the EditText fields
    private EditText codeInput1, codeInput2, codeInput3, codeInput4, codeInput5;
    private MaterialButton btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);  // Make sure this is the correct layout file

        // Initialize the EditText fields
        codeInput1 = findViewById(R.id.codeInput1);
        codeInput2 = findViewById(R.id.codeInput2);
        codeInput3 = findViewById(R.id.codeInput3);
        codeInput4 = findViewById(R.id.codeInput4);
        codeInput5 = findViewById(R.id.codeInput5);
        btnVerify = findViewById(R.id.btnVerify);

        // Set listeners for each EditText to automatically move focus to the next or previous field
        setKeyListener(codeInput1, null, codeInput2);
        setKeyListener(codeInput2, codeInput1, codeInput3);
        setKeyListener(codeInput3, codeInput2, codeInput4);
        setKeyListener(codeInput4, codeInput3, codeInput5);
        setKeyListener(codeInput5, codeInput4, null);

        // Set up the verify button's action
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the verification code from all input fields
                String code = codeInput1.getText().toString() +
                        codeInput2.getText().toString() +
                        codeInput3.getText().toString() +
                        codeInput4.getText().toString() +
                        codeInput5.getText().toString();

                // Check if all digits have been entered
                if (code.length() == 5) {
                    // Check if all characters are digits
                    if (isValidCode(code)) {
                        // Proceed with the verification, e.g., send the code to the server for validation
                        Toast.makeText(VerificationPhoneNumberActivity.this, "Code Verified", Toast.LENGTH_SHORT).show();
                    } else {
                        // If not all characters are digits, show an error
                        Toast.makeText(VerificationPhoneNumberActivity.this, "Please enter a valid numeric code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show an error message to the user if the code is not complete
                    Toast.makeText(VerificationPhoneNumberActivity.this, "Please enter the full verification code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to set up key listeners to automatically move focus
    private void setKeyListener(final EditText currentField, final EditText previousField, final EditText nextField) {
        currentField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Check if the delete key is pressed (keyCode == KEYCODE_DEL)
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (currentField.getText().length() == 0) {
                        // If the current field is empty, move focus to the previous field (if available)
                        if (previousField != null) {
                            previousField.requestFocus();
                        }
                    }
                }

                // If the current field has a digit, move focus to the next field (if available)
                if (currentField.getText().length() == 1 && keyCode != KeyEvent.KEYCODE_DEL) {
                    if (nextField != null) {
                        nextField.requestFocus();
                    }
                }
                return false;
            }
        });
    }

    // Check if the verification code contains only digits
    private boolean isValidCode(String code) {
        // Check if the code contains only numeric digits
        return code.matches("\\d{5}");  // Ensure the code is exactly 5 digits
    }
}
