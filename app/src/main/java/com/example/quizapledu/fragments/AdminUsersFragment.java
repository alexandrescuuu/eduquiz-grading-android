package com.example.quizapledu.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class AdminUsersFragment extends Fragment {

    private LinearLayout containerUsers;
    private TextView tvUserCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        containerUsers = view.findViewById(R.id.container_users);
        tvUserCount    = view.findViewById(R.id.tv_user_count);

        loadUsers();

        return view;
    }

    private void loadUsers() {
        tvUserCount.setText("Se incarca...");

        RetrofitClient.getApiService().getAdminUsers()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            JSONArray users = json.optJSONArray("users");
                            int count = users != null ? users.length() : 0;
                            tvUserCount.setText(count + " utilizatori");

                            containerUsers.removeAllViews();

                            if (users == null) return;

                            // Grupam: admins, teachers, students
                            addSectionHeader("Administratori");
                            addUsersOfRole(users, "admin");
                            addSectionHeader("Profesori");
                            addUsersOfRole(users, "teacher");
                            addSectionHeader("Studenti");
                            addUsersOfRole(users, "student");

                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvUserCount.setText("Eroare conexiune");
                    }
                });
    }

    private void addSectionHeader(String title) {
        TextView tv = new TextView(getActivity());
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#71717A"));
        tv.setTextSize(11f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setAllCaps(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 20, 0, 8);
        tv.setLayoutParams(lp);
        containerUsers.addView(tv);
    }

    private void addUsersOfRole(JSONArray users, String role) throws Exception {
        boolean found = false;
        for (int i = 0; i < users.length(); i++) {
            JSONObject u = users.getJSONObject(i);
            if (!role.equals(u.optString("role"))) continue;
            found = true;

            String name  = u.optString("name", "—");
            String email = u.optString("email", "");
            String uRole = u.optString("role", "student");
            String className = u.optString("class_name", "");

            // Avatar color by role
            String avatarBg    = role.equals("admin")   ? "#3D1515"
                    : role.equals("teacher") ? "#1E1E40"
                    :                          "#2D2250";
            String avatarColor = role.equals("admin")   ? "#F87171"
                    : role.equals("teacher") ? "#60A5FA"
                    :                          "#A78BFA";
            String badgeBg     = role.equals("admin")   ? "#3D1515"
                    : role.equals("teacher") ? "#1E3A5F"
                    :                          "#2D2250";
            String badgeColor  = role.equals("admin")   ? "#F87171"
                    : role.equals("teacher") ? "#60A5FA"
                    :                          "#A78BFA";
            String badgeLabel  = role.equals("admin")   ? "Admin"
                    : role.equals("teacher") ? "Profesor"
                    :                          "Student";

            // Row container
            LinearLayout row = new LinearLayout(getActivity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(20, 16, 20, 16);
            row.setBackgroundColor(Color.parseColor("#18181B"));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, 6);
            row.setLayoutParams(rp);

            // Avatar
            String[] parts = name.split(" ");
            StringBuilder ini = new StringBuilder();
            for (String p : parts) if (!p.isEmpty()) ini.append(p.charAt(0));
            String initials = ini.toString().toUpperCase();

            TextView tvAvatar = new TextView(getActivity());
            tvAvatar.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);
            tvAvatar.setTextColor(Color.parseColor(avatarColor));
            tvAvatar.setTextSize(12f);
            tvAvatar.setTypeface(null, Typeface.BOLD);
            tvAvatar.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(44, 44);
            ap.setMargins(0, 0, 14, 0);
            tvAvatar.setLayoutParams(ap);
            tvAvatar.setBackgroundColor(Color.parseColor(avatarBg));
            row.addView(tvAvatar);

            // Info (name + email/class)
            LinearLayout info = new LinearLayout(getActivity());
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(ip);

            TextView tvName = new TextView(getActivity());
            tvName.setText(name);
            tvName.setTextColor(Color.parseColor("#FAFAFA"));
            tvName.setTextSize(13f);
            info.addView(tvName);

            TextView tvSub = new TextView(getActivity());
            String sub = className.isEmpty() ? email : className + " · " + email;
            tvSub.setText(sub);
            tvSub.setTextColor(Color.parseColor("#52525B"));
            tvSub.setTextSize(11f);
            info.addView(tvSub);

            row.addView(info);

            // Badge rol
            TextView tvBadge = new TextView(getActivity());
            tvBadge.setText(badgeLabel);
            tvBadge.setTextColor(Color.parseColor(badgeColor));
            tvBadge.setTextSize(10f);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setPadding(12, 4, 12, 4);
            tvBadge.setBackgroundColor(Color.parseColor(badgeBg));
            row.addView(tvBadge);

            containerUsers.addView(row);
        }

        if (!found) {
            TextView empty = new TextView(getActivity());
            empty.setText("Niciun " + role + " inregistrat");
            empty.setTextColor(Color.parseColor("#52525B"));
            empty.setTextSize(12f);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.setMargins(0, 4, 0, 4);
            empty.setLayoutParams(ep);
            containerUsers.addView(empty);
        }
    }
}
