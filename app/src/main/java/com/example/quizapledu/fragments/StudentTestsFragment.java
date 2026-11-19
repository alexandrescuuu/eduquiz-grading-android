package com.example.quizapledu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.TestSubmissionActivity;
import com.example.quizapledu.adapters.TestAdapter;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.TestListResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTestsFragment extends Fragment {

    private RecyclerView recyclerTests;
    private TextView tvTestsCount;
    private LinearLayout emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_tests, container, false);

        recyclerTests = view.findViewById(R.id.recycler_tests);
        tvTestsCount = view.findViewById(R.id.tv_tests_count);
        emptyState = view.findViewById(R.id.empty_state);

        recyclerTests.setLayoutManager(new LinearLayoutManager(getActivity()));

        loadTests();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTests();
    }

    private void loadTests() {
        tvTestsCount.setText("Loading...");

        RetrofitClient.getApiService().getTests().enqueue(new Callback<TestListResponse>() {
            @Override
            public void onResponse(Call<TestListResponse> call, Response<TestListResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().tests != null) {

                    java.util.List<TestListResponse.Test> tests = response.body().tests;

                    if (tests.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerTests.setVisibility(View.GONE);
                        tvTestsCount.setText("0 tests pending");
                        return;
                    }

                    emptyState.setVisibility(View.GONE);
                    recyclerTests.setVisibility(View.VISIBLE);
                    tvTestsCount.setText(tests.size() + " tests pending");

                    TestAdapter adapter = new TestAdapter(tests, test -> {
                        // Dialog confirmare inainte de start test
                        String message = "Questions: " + test.question_count
                                + "\nTotal points: " + test.total_points
                                + "\n\nYou can answer by typing, selecting options, "
                                + "or scanning handwritten answers.";

                        new AlertDialog.Builder(requireActivity())
                                .setTitle(test.test_name)
                                .setMessage(message)
                                .setPositiveButton("Start Test", (dialog, which) -> {
                                    Intent intent = new Intent(getActivity(),
                                            TestSubmissionActivity.class);
                                    intent.putExtra("test_id", test.id);
                                    intent.putExtra("test_name", test.test_name);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }, false);

                    recyclerTests.setAdapter(adapter);

                } else {
                    tvTestsCount.setText("Error loading");
                }
            }

            @Override
            public void onFailure(Call<TestListResponse> call, Throwable t) {
                tvTestsCount.setText("Connection error");
                Toast.makeText(getActivity(),
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
