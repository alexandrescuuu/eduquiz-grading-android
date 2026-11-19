package com.example.quizapledu.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.LoginActivity;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.utils.Constants;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class StudentSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_settings, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);

        // ID-urile corecte din fragment_student_settings.xml
        TextView tvName   = view.findViewById(R.id.tv_name);
        TextView tvEmail  = view.findViewById(R.id.tv_email);
        TextView tvAvatar = view.findViewById(R.id.tv_avatar);
        LinearLayout btnLogout         = view.findViewById(R.id.btn_logout);
        LinearLayout btnChangePassword = view.findViewById(R.id.btn_change_password);

        String name  = prefs.getString(Constants.KEY_NAME, "Student");
        String email = prefs.getString(Constants.KEY_EMAIL, "");
        int userId   = prefs.getInt(Constants.KEY_USER_ID, 0);

        if (tvName  != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);

        // Avatar initials
        if (tvAvatar != null) {
            String[] parts = name.split(" ");
            StringBuilder ini = new StringBuilder();
            for (String p : parts) if (!p.isEmpty()) ini.append(p.charAt(0));
            String initials = ini.toString().toUpperCase();
            tvAvatar.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);
        }

        // Schimba parola (optional - poate lipsi din layout)
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog(userId));
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }

    private void showChangePasswordDialog(int userId) {
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_change_password, null);

        EditText etCurrent = dialogView.findViewById(R.id.et_current_password);
        EditText etNew     = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirm = dialogView.findViewById(R.id.et_confirm_password);

        new AlertDialog.Builder(requireActivity())
                .setTitle("Schimba parola")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    String current = etCurrent.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    String confirm = etConfirm.getText().toString().trim();

                    if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                        Toast.makeText(getActivity(), "Completeaza toate campurile",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(getActivity(), "Parolele nu coincid",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 4) {
                        Toast.makeText(getActivity(), "Parola prea scurta (min 4 caractere)",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    changePassword(userId, current, newPass);
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void changePassword(int userId, String currentPassword, String newPassword) {
        RetrofitClient.getApiService().changePassword(userId, currentPassword, newPassword)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(getActivity(),
                                        "Parola schimbata!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        json.optString("error", "Parola curenta gresita"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(getActivity(), "Eroare conexiune", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}