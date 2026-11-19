package com.example.quizapledu.fragments;

import android.graphics.Color;
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
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class AdminOverviewFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalTeachers, tvTotalStudents;
    private TextView tvTotalTests, tvTotalSubmissions, tvAvgPlatform;
    private TextView tvSystemStatus;
    private LinearLayout containerAlerts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_overview, container, false);

        tvTotalUsers      = view.findViewById(R.id.tv_total_users);
        tvTotalTeachers   = view.findViewById(R.id.tv_total_teachers);
        tvTotalStudents   = view.findViewById(R.id.tv_total_students);
        tvTotalTests      = view.findViewById(R.id.tv_total_tests);
        tvTotalSubmissions= view.findViewById(R.id.tv_total_submissions);
        tvAvgPlatform     = view.findViewById(R.id.tv_avg_platform);
        tvSystemStatus    = view.findViewById(R.id.tv_system_status);
        containerAlerts   = view.findViewById(R.id.container_alerts);

        loadOverview();

        return view;
    }

    private void loadOverview() {
        RetrofitClient.getApiService().getAdminOverview()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            tvTotalUsers.setText(String.valueOf(json.optInt("total_users", 0)));
                            tvTotalTeachers.setText(String.valueOf(json.optInt("total_teachers", 0)));
                            tvTotalStudents.setText(String.valueOf(json.optInt("total_students", 0)));
                            tvTotalTests.setText(String.valueOf(json.optInt("total_tests", 0)));
                            tvTotalSubmissions.setText(String.valueOf(json.optInt("total_submissions", 0)));

                            double avg = json.optDouble("platform_avg", Double.NaN);
                            tvAvgPlatform.setText(!Double.isNaN(avg)
                                    ? String.format("%.1f%%", avg) : "--");

                            // Status sistem
                            tvSystemStatus.setText("● Operational");
                            tvSystemStatus.setTextColor(Color.parseColor("#34D399"));

                            // Alerte
                            containerAlerts.removeAllViews();
                            int noSubmission = json.optInt("tests_no_submissions", 0);
                            if (noSubmission > 0) {
                                addAlert(containerAlerts,
                                        noSubmission + " teste fara nicio submisie",
                                        "#FBBF24");
                            }
                            int usersNoClass = json.optInt("students_no_class", 0);
                            if (usersNoClass > 0) {
                                addAlert(containerAlerts,
                                        usersNoClass + " studenti fara clasa asignata",
                                        "#F87171");
                            }
                            if (noSubmission == 0 && usersNoClass == 0) {
                                addAlert(containerAlerts, "Nicio alerta activa", "#34D399");
                            }

                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvSystemStatus.setText("● Offline");
                        tvSystemStatus.setTextColor(Color.parseColor("#F87171"));
                    }
                });
    }

    private void addAlert(LinearLayout container, String message, String color) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rp);
        row.setPadding(24, 18, 24, 18);
        row.setBackgroundColor(Color.parseColor("#18181B"));

        View dot = new View(getActivity());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(10, 10);
        dp.setMargins(0, 14, 16, 0);
        dot.setLayoutParams(dp);
        dot.setBackgroundColor(Color.parseColor(color));
        row.addView(dot);

        TextView tv = new TextView(getActivity());
        tv.setText(message);
        tv.setTextColor(Color.parseColor("#D4D4D8"));
        tv.setTextSize(13f);
        row.addView(tv);

        container.addView(row);
    }
}
