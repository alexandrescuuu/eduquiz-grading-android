package com.example.quizapledu.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.fragments.TeacherHomeFragment;
import com.example.quizapledu.fragments.TeacherClassesFragment;
import com.example.quizapledu.fragments.TeacherScanFragment;
import com.example.quizapledu.fragments.TeacherAnalyticsFragment;
import com.example.quizapledu.fragments.TeacherSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Incarca primul fragment (Dashboard/Home)
        if (savedInstanceState == null) {
            loadFragment(new TeacherHomeFragment());
        }

        // Listener pentru navigare
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new TeacherHomeFragment();
            } else if (itemId == R.id.nav_classes) {
                fragment = new TeacherClassesFragment();
            } else if (itemId == R.id.nav_scan) {
                fragment = new TeacherScanFragment();
            } else if (itemId == R.id.nav_analytics) {
                fragment = new TeacherAnalyticsFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new TeacherSettingsFragment();
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
