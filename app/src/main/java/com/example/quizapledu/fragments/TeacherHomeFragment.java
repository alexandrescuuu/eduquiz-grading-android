package com.example.quizapledu.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.ScanActivity;
import com.example.quizapledu.activities.TestCreationActivity;
import com.example.quizapledu.activities.TestListActivity;
import com.example.quizapledu.adapters.TestAdapter;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.TestListResponse;
import com.example.quizapledu.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import java.util.ArrayList;
import java.util.List;

public class TeacherHomeFragment extends Fragment {

    private TextView tvTeacherName, tvTestsCorrected, tvClassAverage;
    private TextView tvActiveStudents, tvPendingScans, tvAvatar;
    private Button btnCreateTest, btnViewTests, btnScanFull, btnScanOmr;
    private RecyclerView rvRecentTests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        tvTeacherName = view.findViewById(R.id.tv_teacher_name);
        tvTestsCorrected = view.findViewById(R.id.tv_tests_corrected);
        tvClassAverage = view.findViewById(R.id.tv_class_average);
        tvActiveStudents = view.findViewById(R.id.tv_active_students);
        tvPendingScans = view.findViewById(R.id.tv_pending_scans);
        tvAvatar = view.findViewById(R.id.tv_avatar);
        btnCreateTest = view.findViewById(R.id.btn_create_test);
        btnViewTests = view.findViewById(R.id.btn_view_tests);
        btnScanFull = view.findViewById(R.id.btn_scan_full);
        btnScanOmr = view.findViewById(R.id.btn_scan_omr);
        rvRecentTests = view.findViewById(R.id.rv_recent_tests);
        rvRecentTests.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvRecentTests.setNestedScrollingEnabled(false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        String name = prefs.getString(Constants.KEY_NAME, "Teacher");
        tvTeacherName.setText(name);

        String[] parts = name.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) initials.append(p.charAt(0));
        }
        String ini = initials.toString().toUpperCase();
        tvAvatar.setText(ini.length() >= 2 ? ini.substring(0, 2) : ini);

        btnCreateTest.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), TestCreationActivity.class)));
        btnViewTests.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), TestListActivity.class)));
        btnScanFull.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScanActivity.class);
            intent.putExtra("scan_mode", "full_page");
            startActivity(intent);
        });
        btnScanOmr.setOnClickListener(v ->
                Toast.makeText(getActivity(), "OMR scanner coming soon", Toast.LENGTH_SHORT).show());

        loadTeacherDashboard(prefs);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        loadTeacherDashboard(prefs);
    }

    private void loadTeacherDashboard(SharedPreferences prefs) {
        int teacherId = prefs.getInt(Constants.KEY_USER_ID, 1);

        RetrofitClient.getApiService().getTeacherDashboard(teacherId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            // Statistici
                            int testsCreated = json.optInt("tests_created", 0);
                            tvTestsCorrected.setText(String.valueOf(testsCreated));

                            double avgScore = json.optDouble("avg_score", Double.NaN);
                            if (!Double.isNaN(avgScore)) {
                                tvClassAverage.setText(String.format("%.0f%%", avgScore));
                            } else {
                                tvClassAverage.setText("--");
                            }

                            int activeStudents = json.optInt("active_students", 0);
                            tvActiveStudents.setText(String.valueOf(activeStudents));

                            int totalSubmissions = json.optInt("total_submissions", 0);
                            tvPendingScans.setText(String.valueOf(totalSubmissions));

                            // Teste recente in RecyclerView
                            JSONArray recentArr = json.optJSONArray("recent_tests");
                            List<TestListResponse.Test> recentTests = new ArrayList<>();
                            if (recentArr != null) {
                                for (int i = 0; i < recentArr.length(); i++) {
                                    JSONObject t = recentArr.getJSONObject(i);
                                    TestListResponse.Test test = new TestListResponse.Test();
                                    test.id = t.optInt("id");
                                    test.test_name = t.optString("test_name");
                                    test.question_count = t.optInt("question_count", 0);
                                    test.submission_count = t.optInt("submission_count", 0);
                                    double tAvg = t.optDouble("avg_score", Double.NaN);
                                    test.avg_score = Double.isNaN(tAvg) ? null : tAvg;
                                    recentTests.add(test);
                                }
                            }

                            TestAdapter adapter = new TestAdapter(recentTests,
                                    test -> startActivity(new Intent(getActivity(), TestListActivity.class)),
                                    true);
                            rvRecentTests.setAdapter(adapter);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // Lasam valorile la 0
                    }
                });
    }
}
