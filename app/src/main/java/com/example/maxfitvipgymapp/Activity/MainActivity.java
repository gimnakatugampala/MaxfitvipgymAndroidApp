package com.example.maxfitvipgymapp.Activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.maxfitvipgymapp.R;
import com.example.maxfitvipgymapp.Fragments.HomeFragment;
import com.example.maxfitvipgymapp.Fragments.InsightsFragment;
import com.example.maxfitvipgymapp.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // HashMap to map menu item IDs to fragments
    private HashMap<Integer, Fragment> fragmentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Initialize the fragment map
        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_insights, new InsightsFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        // Set Home Fragment as default when the app opens
        loadFragment(fragmentMap.get(R.id.nav_home));

        // Set item selected listener for bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Use the map to get the selected fragment based on the item ID
            Fragment selectedFragment = fragmentMap.get(item.getItemId());

            // Return true if fragment is loaded
            return selectedFragment != null && loadFragment(selectedFragment);
        });
    }

    // Method to load a fragment into the container
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
