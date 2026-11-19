package com.example.quizapledu.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.TestSubmissionActivity;
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

public class StudentHomeFragment extends Fragment {

    private TextView tvStudentName, tvStudentClass, tvAvatar;
    private TextView tvAvgScore, tvTestsTaken;
    private TextView tvBestSubject, tvBestScore, tvWorstSubject, tvWorstScore;
    private RecyclerView rvRecentTests;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        tvStudentName = view.findViewById(R.id.tv_student_name);
        tvStudentClass = view.findViewById(R.id.tv_student_class);
        tvAvatar = view.findViewById(R.id.tv_avatar);
        tvAvgScore = view.findViewById(R.id.tv_avg_score);
        tvTestsTaken = view.findViewById(R.id.tv_tests_taken);
        tvBestSubject = view.findViewById(R.id.tv_best_subject);
        tvBestScore = view.findViewById(R.id.tv_best_score);
        tvWorstSubject = view.findViewById(R.id.tv_worst_subject);
        tvWorstScore = view.findViewById(R.id.tv_worst_score);
        rvRecentTests = view.findViewById(R.id.rv_recent_tests);
        rvRecentTests.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvRecentTests.setNestedScrollingEnabled(false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        String name = prefs.getString(Constants.KEY_NAME, "Student");
        tvStudentName.setText(name);

        // Avatar initials
        String[] parts = name.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) initials.append(p.charAt(0));
        }
        String ini = initials.toString().toUpperCase();
        tvAvatar.setText(ini.length() >= 2 ? ini.substring(0, 2) : ini);

        loadStudentDashboard(prefs);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        loadStudentDashboard(prefs);
    }

    private void loadStudentDashboard(SharedPreferences prefs) {
        int studentId = prefs.getInt(Constants.KEY_USER_ID, 0);

        RetrofitClient.getApiService().getStudentDashboard(studentId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            // Clasa
                            String className = json.optString("class_name", "");
                            if (!className.isEmpty() && tvStudentClass != null) {
                                tvStudentClass.setText(className);
                            }

                            // Statistici personale
                            int testsTaken = json.optInt("tests_taken", 0);
                            tvTestsTaken.setText(String.valueOf(testsTaken));

                            double avgScore = json.optDouble("avg_score", Double.NaN);
                            if (!Double.isNaN(avgScore)) {
                                tvAvgScore.setText(String.format("%.0f%%", avgScore));
                            } else {
                                tvAvgScore.setText("--");
                            }

                            // Cel mai bun test
                            JSONObject bestTest = json.optJSONObject("best_test");
                            if (bestTest != null) {
                                String bName = bestTest.optString("test_name", "--");
                                double bScore = bestTest.optDouble("score", 0);
                                tvBestSubject.setText(bName.length() > 16 ? bName.substring(0, 14) + "..." : bName);
                                tvBestScore.setText(String.format("%.0f%%", bScore));
                            } else {
                                tvBestSubject.setText("--");
                                tvBestScore.setText("--");
                            }

                            // Cel mai slab test
                            JSONObject worstTest = json.optJSONObject("worst_test");
                            if (worstTest != null) {
                                String wName = worstTest.optString("test_name", "--");
                                double wScore = worstTest.optDouble("score", 0);
                                tvWorstSubject.setText(wName.length() > 16 ? wName.substring(0, 14) + "..." : wName);
                                tvWorstScore.setText(String.format("%.0f%%", wScore));
                            } else {
                                tvWorstSubject.setText("--");
                                tvWorstScore.setText("--");
                            }

                            // Teste disponibile in RecyclerView
                            JSONArray availableArr = json.optJSONArray("available_tests");
                            List<TestListResponse.Test> availableTests = new ArrayList<>();
                            if (availableArr != null) {
                                for (int i = 0; i < availableArr.length(); i++) {
                                    JSONObject t = availableArr.getJSONObject(i);
                                    TestListResponse.Test test = new TestListResponse.Test();
                                    test.id = t.optInt("id");
                                    test.test_name = t.optString("test_name");
                                    test.question_count = t.optInt("question_count");
                                    availableTests.add(test);
                                }
                            }

                            TestAdapter adapter = new TestAdapter(availableTests, test -> {
                                Intent intent = new Intent(getActivity(), TestSubmissionActivity.class);
                                intent.putExtra("test_id", test.id);
                                intent.putExtra("test_name", test.test_name);
                                startActivity(intent);
                            }, false);
                            rvRecentTests.setAdapter(adapter);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvAvgScore.setText("--");
                        tvTestsTaken.setText("0");
                    }
                });
    }
}
