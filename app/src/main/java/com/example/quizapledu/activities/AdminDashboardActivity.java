package com.example.quizapledu.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.fragments.AdminOverviewFragment;
import com.example.quizapledu.fragments.AdminUsersFragment;
import com.example.quizapledu.fragments.AdminReportsFragment;
import com.example.quizapledu.fragments.AdminSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new AdminOverviewFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_overview) {
                fragment = new AdminOverviewFragment();
            } else if (id == R.id.nav_users) {
                fragment = new AdminUsersFragment();
            } else if (id == R.id.nav_reports) {
                fragment = new AdminReportsFragment();
            } else if (id == R.id.nav_admin_settings) {
                fragment = new AdminSettingsFragment();
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
