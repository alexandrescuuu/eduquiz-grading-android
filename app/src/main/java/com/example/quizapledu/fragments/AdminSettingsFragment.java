package com.example.quizapledu.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.LoginActivity;
import com.example.quizapledu.utils.Constants;

public class AdminSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_settings, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);

        TextView tvAdminName  = view.findViewById(R.id.tv_admin_name);
        TextView tvAdminEmail = view.findViewById(R.id.tv_admin_email);
        TextView tvAdminAvatar = view.findViewById(R.id.tv_admin_avatar);
        Button btnLogout      = view.findViewById(R.id.btn_logout);

        String name  = prefs.getString(Constants.KEY_NAME, "Admin");
        String email = prefs.getString(Constants.KEY_EMAIL, "");

        tvAdminName.setText(name);
        tvAdminEmail.setText(email);

        String[] parts = name.split(" ");
        StringBuilder ini = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) ini.append(p.charAt(0));
        String initials = ini.toString().toUpperCase();
        tvAdminAvatar.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
