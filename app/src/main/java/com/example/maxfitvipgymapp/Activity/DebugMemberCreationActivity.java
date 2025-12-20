package com.example.maxfitvipgymapp.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxfitvipgymapp.Config.SupabaseConfig;
import com.example.maxfitvipgymapp.Model.Member;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Repository.MemberRepository;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugMemberCreationActivity extends AppCompatActivity {

    private TextView resultText;
    private EditText phoneInput;
    private EditText membershipInput;
    private EditText firstNameInput;
    private EditText lastNameInput;
    private Button btnTestCreate;
    private Button btnTestConnection;
    private ScrollView scrollView;
    private ExecutorService executorService;
    private MemberRepository memberRepository;
    private static final String TAG = "DebugMemberCreation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a simple layout programmatically
        ScrollView scrollView = new ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // Title
        TextView title = new TextView(this);
        title.setText("Debug Member Creation");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 32);
        layout.addView(title);

        // Config info
        TextView configInfo = new TextView(this);
        configInfo.setText("Supabase URL: " + SupabaseConfig.SUPABASE_URL + "\n" +
                "Using Table: " + SupabaseConfig.TABLE_MEMBERS);
        configInfo.setPadding(0, 0, 0, 16);
        layout.addView(configInfo);

        // Phone input
        TextView phoneLabel = new TextView(this);
        phoneLabel.setText("Phone Number:");
        layout.addView(phoneLabel);

        phoneInput = new EditText(this);
        phoneInput.setHint("Enter phone number");
        phoneInput.setText("1234567890");
        layout.addView(phoneInput);

        // Membership ID input
        TextView membershipLabel = new TextView(this);
        membershipLabel.setText("Membership ID:");
        membershipLabel.setPadding(0, 16, 0, 0);
        layout.addView(membershipLabel);

        membershipInput = new EditText(this);
        membershipInput.setHint("Enter membership ID");
        membershipInput.setText("TEST001");
        layout.addView(membershipInput);

        // First Name input
        TextView firstNameLabel = new TextView(this);
        firstNameLabel.setText("First Name:");
        firstNameLabel.setPadding(0, 16, 0, 0);
        layout.addView(firstNameLabel);

        firstNameInput = new EditText(this);
        firstNameInput.setHint("Enter first name");
        firstNameInput.setText("Test");
        layout.addView(firstNameInput);

        // Last Name input
        TextView lastNameLabel = new TextView(this);
        lastNameLabel.setText("Last Name:");
        lastNameLabel.setPadding(0, 16, 0, 0);
        layout.addView(lastNameLabel);

        lastNameInput = new EditText(this);
        lastNameInput.setHint("Enter last name");
        lastNameInput.setText("User");
        layout.addView(lastNameInput);

        // Test create button
        btnTestCreate = new Button(this);
        btnTestCreate.setText("Test Create Member");
        btnTestCreate.setPadding(0, 16, 0, 0);
        layout.addView(btnTestCreate);

        // Test connection button
        btnTestConnection = new Button(this);
        btnTestConnection.setText("Test Basic Connection");
        layout.addView(btnTestConnection);

        // Result text
        TextView resultLabel = new TextView(this);
        resultLabel.setText("\nResults:");
        resultLabel.setTextSize(18);
        resultLabel.setPadding(0, 32, 0, 8);
        layout.addView(resultLabel);

        resultText = new TextView(this);
        resultText.setTextIsSelectable(true);
        resultText.setPadding(16, 16, 16, 16);
        resultText.setBackgroundColor(0xFF333333);
        resultText.setTextColor(0xFFFFFFFF);
        layout.addView(resultText);

        scrollView.addView(layout);
        setContentView(scrollView);

        executorService = Executors.newSingleThreadExecutor();
        memberRepository = new MemberRepository();

        // Set up button listeners
        btnTestConnection.setOnClickListener(v -> testBasicConnection());
        btnTestCreate.setOnClickListener(v -> testCreateMember());
    }

    private void testBasicConnection() {
        appendResult("Testing basic connection...\n");

        executorService.execute(() -> {
            try {
                List<Member> members = memberRepository.getAllActiveMembers();

                runOnUiThread(() -> {
                    if (members != null) {
                        appendResult("✅ SUCCESS! Found " + members.size() + " active members\n");
                        Toast.makeText(this, "Connection successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        appendResult("❌ FAILED: Got null response\n");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendResult("❌ ERROR: " + e.getMessage() + "\n");
                    Log.e(TAG, "Error", e);
                });
            }
        });
    }

    private void testCreateMember() {
        String phone = phoneInput.getText().toString().trim();
        String membershipId = membershipInput.getText().toString().trim();
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();

        if (phone.isEmpty() || membershipId.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        appendResult("\n--- Testing Member Creation ---\n");
        appendResult("Phone: " + phone + "\n");
        appendResult("Membership ID: " + membershipId + "\n");
        appendResult("Name: " + firstName + " " + lastName + "\n");

        executorService.execute(() -> {
            try {
                // First check if member already exists
                appendResult("Checking if member exists...\n");
                Member existingMember = memberRepository.getMemberByPhone(phone);

                if (existingMember != null) {
                    runOnUiThread(() -> {
                        appendResult("⚠️ Member already exists with this phone!\n");
                        appendResult("Existing Member ID: " + existingMember.getId() + "\n");
                        appendResult("Existing Membership ID: " + existingMember.getMembershipId() + "\n");
                    });
                    return;
                }

                // Check if membership ID exists
                appendResult("Checking if membership ID exists...\n");
                Member existingMembership = memberRepository.getMemberByMembershipId(membershipId);

                if (existingMembership != null) {
                    runOnUiThread(() -> {
                        appendResult("⚠️ Membership ID already in use!\n");
                        appendResult("Used by: " + existingMembership.getFirstName() + " " + existingMembership.getLastName() + "\n");
                    });
                    return;
                }

                appendResult("Creating new member...\n");

                // Create new member
                Member newMember = new Member();
                newMember.setMembershipId(membershipId);
                newMember.setFirstName(firstName);
                newMember.setLastName(lastName);
                newMember.setPhoneNumber(phone);
                newMember.setActive(true);
                newMember.setDeleted(false);
                newMember.setPlatformId(2);

                // Log the JSON that will be sent
                try {
                    JSONObject json = newMember.toJSON();
                    appendResult("JSON to send:\n" + json.toString(2) + "\n");
                } catch (Exception e) {
                    appendResult("Error creating JSON: " + e.getMessage() + "\n");
                }

                Member createdMember = memberRepository.createMember(newMember);

                runOnUiThread(() -> {
                    if (createdMember != null) {
                        appendResult("✅ SUCCESS! Member created\n");
                        appendResult("ID: " + createdMember.getId() + "\n");
                        appendResult("Name: " + createdMember.getFirstName() + " " + createdMember.getLastName() + "\n");
                        appendResult("Membership ID: " + createdMember.getMembershipId() + "\n");
                        appendResult("Phone: " + createdMember.getPhoneNumber() + "\n");
                        Toast.makeText(this, "Member created successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        appendResult("❌ FAILED: createMember returned null\n");
                        appendResult("Check LogCat for more details\n");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendResult("❌ ERROR: " + e.getMessage() + "\n");
                    appendResult("Stack trace:\n");
                    for (StackTraceElement element : e.getStackTrace()) {
                        appendResult("  " + element.toString() + "\n");
                    }
                    Log.e(TAG, "Error creating member", e);
                });
            }
        });
    }

    private void appendResult(String text) {
        runOnUiThread(() -> {
            resultText.append(text);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
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