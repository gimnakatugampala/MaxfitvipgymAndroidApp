package com.example.maxfitvipgymapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Repository.MemberRepository;
import com.example.maxfitvipgymapp.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipDetailsActivity extends AppCompatActivity {

    private static final String TAG = "MembershipDetails";

    private EditText membershipIDInput, firstNameInput, lastNameInput;
    private MaterialButton btnFinish;
    private TextView title;
    private MemberRepository memberRepository;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private String phoneNumber;
    private int memberId;
    private boolean memberExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_details);

        // Initialize views
        title = findViewById(R.id.title);
        membershipIDInput = findViewById(R.id.membershipIDInput);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        btnFinish = findViewById(R.id.btnFinish);

        memberRepository = new MemberRepository();
        sessionManager = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();

        // Get data from intent
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        memberId = getIntent().getIntExtra("memberId", -1);
        memberExists = getIntent().getBooleanExtra("memberExists", false);

        Log.d(TAG, "onCreate - Phone: " + phoneNumber);
        Log.d(TAG, "onCreate - MemberId: " + memberId);
        Log.d(TAG, "onCreate - MemberExists: " + memberExists);

        // Validate phone number was passed
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Error: Phone number not provided", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Phone number is null or empty!");
            finish();
            return;
        }

        // Update UI based on whether member exists
        if (memberExists && memberId != -1) {
            title.setText("Welcome Back!");
            btnFinish.setText("Login");
            loadMemberData();
        } else {
            title.setText("Create Your Profile");
            btnFinish.setText("Register");
        }

        // Set up the finish button click listener
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String membershipID = membershipIDInput.getText().toString().trim();
                String firstName = firstNameInput.getText().toString().trim();
                String lastName = lastNameInput.getText().toString().trim();

                if (membershipID.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                    Toast.makeText(MembershipDetailsActivity.this,
                            "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (memberExists) {
                    // Existing member - verify details
                    verifyAndLogin(membershipID, firstName, lastName);
                } else {
                    // New member - create account
                    createNewMember(membershipID, firstName, lastName);
                }
            }
        });
    }

    private void loadMemberData() {
        executorService.execute(() -> {
            try {
                Member member = memberRepository.getMemberById(memberId);
                if (member != null) {
                    runOnUiThread(() -> {
                        membershipIDInput.setText(member.getMembershipId());
                        firstNameInput.setText(member.getFirstName());
                        lastNameInput.setText(member.getLastName());
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading member data", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading member data", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void createNewMember(String membershipID, String firstName, String lastName) {
        btnFinish.setEnabled(false);

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Creating new member:");
                Log.d(TAG, "Phone: " + phoneNumber);
                Log.d(TAG, "Membership ID: " + membershipID);
                Log.d(TAG, "Name: " + firstName + " " + lastName);

                // Check if membership ID already exists
                Member existingMember = memberRepository.getMemberByMembershipId(membershipID);

                if (existingMember != null) {
                    Log.e(TAG, "Membership ID already exists");
                    runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        Toast.makeText(MembershipDetailsActivity.this,
                                "Membership ID already in use. Please use a different one.",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Create new member
                Member newMember = new Member();
                newMember.setMembershipId(membershipID);
                newMember.setFirstName(firstName);
                newMember.setLastName(lastName);
                newMember.setPhoneNumber(phoneNumber);
                newMember.setActive(true);
                newMember.setDeleted(false);
                newMember.setPlatformId(2); // Android platform (assuming 2 is for mobile)

                Log.d(TAG, "Attempting to create member in database...");
                Member createdMember = memberRepository.createMember(newMember);

                if (createdMember != null) {
                    Log.d(TAG, "Member created successfully with ID: " + createdMember.getId());

                    runOnUiThread(() -> {
                        // Save session
                        sessionManager.createLoginSession(createdMember);

                        Toast.makeText(MembershipDetailsActivity.this,
                                "Account created successfully! Welcome " + firstName,
                                Toast.LENGTH_SHORT).show();

                        // Navigate to main activity
                        Intent intent = new Intent(MembershipDetailsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Log.e(TAG, "Failed to create member - createMember returned null");
                    runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        Toast.makeText(MembershipDetailsActivity.this,
                                "Failed to create account. Please check your internet connection and try again.",
                                Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating member", e);
                runOnUiThread(() -> {
                    btnFinish.setEnabled(true);
                    Toast.makeText(MembershipDetailsActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void verifyAndLogin(String membershipID, String firstName, String lastName) {
        btnFinish.setEnabled(false);

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Attempting to verify:");
                Log.d(TAG, "Phone: " + phoneNumber);
                Log.d(TAG, "Membership ID: " + membershipID);
                Log.d(TAG, "First Name: " + firstName);
                Log.d(TAG, "Last Name: " + lastName);

                // First, try to get member by phone
                Member memberByPhone = memberRepository.getMemberByPhone(phoneNumber);

                if (memberByPhone == null) {
                    Log.e(TAG, "No member found with phone: " + phoneNumber);
                    runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        Toast.makeText(MembershipDetailsActivity.this,
                                "No member found with this phone number",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Log.d(TAG, "Found member: " + memberByPhone.getFirstName() + " " + memberByPhone.getLastName());
                Log.d(TAG, "DB Membership ID: " + memberByPhone.getMembershipId());

                // Check if membership ID matches
                if (!memberByPhone.getMembershipId().equalsIgnoreCase(membershipID)) {
                    Log.e(TAG, "Membership ID mismatch");
                    runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        Toast.makeText(MembershipDetailsActivity.this,
                                "Membership ID doesn't match. Expected: " + memberByPhone.getMembershipId(),
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Check if name matches (case insensitive, trim whitespace)
                String dbFirstName = memberByPhone.getFirstName().trim();
                String dbLastName = memberByPhone.getLastName().trim();
                String inputFirstName = firstName.trim();
                String inputLastName = lastName.trim();

                boolean firstNameMatches = dbFirstName.equalsIgnoreCase(inputFirstName);
                boolean lastNameMatches = dbLastName.equalsIgnoreCase(inputLastName);

                if (!firstNameMatches || !lastNameMatches) {
                    Log.e(TAG, "Name mismatch");
                    Log.e(TAG, "Expected: " + dbFirstName + " " + dbLastName);
                    Log.e(TAG, "Got: " + inputFirstName + " " + inputLastName);

                    runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        Toast.makeText(MembershipDetailsActivity.this,
                                "Name doesn't match. Expected: " + dbFirstName + " " + dbLastName,
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // All checks passed - login successful
                Log.d(TAG, "Verification successful!");

                // Update last active
                memberRepository.updateLastActive(memberByPhone.getId());

                runOnUiThread(() -> {
                    // Save session
                    sessionManager.createLoginSession(memberByPhone);

                    Toast.makeText(MembershipDetailsActivity.this,
                            "Login successful! Welcome back " + memberByPhone.getFirstName(),
                            Toast.LENGTH_SHORT).show();

                    // Navigate to main activity
                    Intent intent = new Intent(MembershipDetailsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during verification", e);
                runOnUiThread(() -> {
                    btnFinish.setEnabled(true);
                    Toast.makeText(MembershipDetailsActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}