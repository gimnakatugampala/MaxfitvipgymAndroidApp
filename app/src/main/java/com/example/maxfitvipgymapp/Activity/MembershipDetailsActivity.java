package com.example.maxfitvipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.R;
import com.google.android.material.button.MaterialButton;

public class MembershipDetailsActivity extends AppCompatActivity {

    private EditText membershipIDInput, firstNameInput, lastNameInput;
    private MaterialButton btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_details);

        // Initialize views
        membershipIDInput = findViewById(R.id.membershipIDInput);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        btnFinish = findViewById(R.id.btnFinish);

        // Set up the finish button click listener
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the values from the input fields
                String membershipID = membershipIDInput.getText().toString().trim();
                String firstName = firstNameInput.getText().toString().trim();
                String lastName = lastNameInput.getText().toString().trim();

                // Validate if all fields are filled
                if (membershipID.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                    // Show a toast if any field is empty
                    Toast.makeText(MembershipDetailsActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If all fields are filled, proceed to the next screen
                // For example, navigate to the Home screen or any other screen
                Intent intent = new Intent(MembershipDetailsActivity.this, MembershipDetailsActivity.class);  // Replace 'HomeActivity' with your actual activity
                startActivity(intent);
                finish();  // Close this activity to prevent going back
            }
        });
    }
}
