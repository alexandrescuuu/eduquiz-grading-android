package com.example.quizapledu.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.fragments.StudentHomeFragment;
import com.example.quizapledu.fragments.StudentTestsFragment;
import com.example.quizapledu.fragments.StudentScanFragment;
import com.example.quizapledu.fragments.StudentProgressFragment;
import com.example.quizapledu.fragments.StudentSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new StudentHomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new StudentHomeFragment();
            } else if (itemId == R.id.nav_tests) {
                fragment = new StudentTestsFragment();
            } else if (itemId == R.id.nav_scan) {
                fragment = new StudentScanFragment();
            } else if (itemId == R.id.nav_progress) {
                fragment = new StudentProgressFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new StudentSettingsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
