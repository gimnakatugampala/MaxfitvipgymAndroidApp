package com.example.maxfitvipgymapp.Activity;

import android.os.Bundle;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestConnectionActivity extends AppCompatActivity {

    private TextView resultText;
    private EditText phoneInput;
    private EditText membershipInput;
    private Button btnTestConnection;
    private Button btnTestPhone;
    private Button btnTestMembership;
    private Button btnListMembers;
    private ScrollView scrollView;
    private ExecutorService executorService;
    private MemberRepository memberRepository;

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
        title.setText("Supabase Connection Test");
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
        phoneLabel.setText("Test Phone Number:");
        layout.addView(phoneLabel);

        phoneInput = new EditText(this);
        phoneInput.setHint("Enter phone number");
        layout.addView(phoneInput);

        btnTestPhone = new Button(this);
        btnTestPhone.setText("Test Get Member by Phone");
        layout.addView(btnTestPhone);

        // Membership ID input
        TextView membershipLabel = new TextView(this);
        membershipLabel.setText("Test Membership ID:");
        membershipLabel.setPadding(0, 16, 0, 0);
        layout.addView(membershipLabel);

        membershipInput = new EditText(this);
        membershipInput.setHint("Enter membership ID");
        layout.addView(membershipInput);

        btnTestMembership = new Button(this);
        btnTestMembership.setText("Test Get Member by Membership ID");
        layout.addView(btnTestMembership);

        // List members button
        btnListMembers = new Button(this);
        btnListMembers.setText("List All Active Members");
        btnListMembers.setPadding(0, 16, 0, 0);
        layout.addView(btnListMembers);

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
        btnTestPhone.setOnClickListener(v -> testGetByPhone());
        btnTestMembership.setOnClickListener(v -> testGetByMembershipId());
        btnListMembers.setOnClickListener(v -> listAllMembers());
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
                    android.util.Log.e("TestConnection", "Error", e);
                });
            }
        });
    }

    private void testGetByPhone() {
        String phone = phoneInput.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        appendResult("\n--- Testing Phone Lookup ---\n");
        appendResult("Searching for: " + phone + "\n");

        executorService.execute(() -> {
            try {
                Member member = memberRepository.getMemberByPhone(phone);

                runOnUiThread(() -> {
                    if (member != null) {
                        appendResult("✅ FOUND!\n");
                        appendResult("ID: " + member.getId() + "\n");
                        appendResult("Name: " + member.getFirstName() + " " + member.getLastName() + "\n");
                        appendResult("Membership ID: " + member.getMembershipId() + "\n");
                        appendResult("Phone: " + member.getPhoneNumber() + "\n");
                        appendResult("Active: " + member.isActive() + "\n");
                        Toast.makeText(this, "Member found!", Toast.LENGTH_SHORT).show();
                    } else {
                        appendResult("❌ NOT FOUND\n");
                        appendResult("No member with phone: " + phone + "\n");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendResult("❌ ERROR: " + e.getMessage() + "\n");
                    android.util.Log.e("TestConnection", "Error getting member by phone", e);
                });
            }
        });
    }

    private void testGetByMembershipId() {
        String membershipId = membershipInput.getText().toString().trim();

        if (membershipId.isEmpty()) {
            Toast.makeText(this, "Please enter a membership ID", Toast.LENGTH_SHORT).show();
            return;
        }

        appendResult("\n--- Testing Membership ID Lookup ---\n");
        appendResult("Searching for: " + membershipId + "\n");

        executorService.execute(() -> {
            try {
                Member member = memberRepository.getMemberByMembershipId(membershipId);

                runOnUiThread(() -> {
                    if (member != null) {
                        appendResult("✅ FOUND!\n");
                        appendResult("ID: " + member.getId() + "\n");
                        appendResult("Name: " + member.getFirstName() + " " + member.getLastName() + "\n");
                        appendResult("Membership ID: " + member.getMembershipId() + "\n");
                        appendResult("Phone: " + member.getPhoneNumber() + "\n");
                        appendResult("Active: " + member.isActive() + "\n");
                        Toast.makeText(this, "Member found!", Toast.LENGTH_SHORT).show();
                    } else {
                        appendResult("❌ NOT FOUND\n");
                        appendResult("No member with membership ID: " + membershipId + "\n");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendResult("❌ ERROR: " + e.getMessage() + "\n");
                    android.util.Log.e("TestConnection", "Error getting member by membership ID", e);
                });
            }
        });
    }

    private void listAllMembers() {
        appendResult("\n--- Listing All Active Members ---\n");

        executorService.execute(() -> {
            try {
                List<Member> members = memberRepository.getAllActiveMembers();

                runOnUiThread(() -> {
                    if (members != null && !members.isEmpty()) {
                        appendResult("Found " + members.size() + " members:\n\n");

                        for (Member member : members) {
                            appendResult("---\n");
                            appendResult("ID: " + member.getId() + "\n");
                            appendResult("Name: " + member.getFirstName() + " " + member.getLastName() + "\n");
                            appendResult("Membership ID: " + member.getMembershipId() + "\n");
                            appendResult("Phone: " + member.getPhoneNumber() + "\n\n");
                        }

                        Toast.makeText(this, "Listed " + members.size() + " members", Toast.LENGTH_SHORT).show();
                    } else {
                        appendResult("No active members found\n");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendResult("❌ ERROR: " + e.getMessage() + "\n");
                    android.util.Log.e("TestConnection", "Error listing members", e);
                });
            }
        });
    }

    private void appendResult(String text) {
        resultText.append(text);
        // Auto-scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}